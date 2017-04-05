/*
 * Copyright (c) 2016 Georgi Neykov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud;

import com.pcloud.protocol.streaming.*;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;

import static com.pcloud.internal.ClientIOUtils.closeQuietly;

class RealCall implements Call {

    private Request request;
    private volatile boolean cancelled;
    private volatile boolean executed;
    private RealConnection connection;

    private ConnectionPool connectionPool;
    private ConnectionFactory connectionFactory;
    private boolean eagerlyCheckConnectivity;

    RealCall(Request request, ConnectionPool connectionPool, ConnectionFactory connectionFactory, boolean eagerlyCheckConnectivity) {
        this.request = request;
        this.connectionPool = connectionPool;
        this.connectionFactory = connectionFactory;
        this.eagerlyCheckConnectivity = eagerlyCheckConnectivity;
    }

    @Override
    public Response execute() throws IOException {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }

        if (cancelled) {
            throw new IOException("Cancelled.");
        }

        connection = obtainConnection();
        System.out.println("Making request, using connection " + connection);
        boolean success = false;
        try {
            ProtocolRequestWriter writer = new BytesWriter(connection.sink());
            writer.beginRequest()
                    .writeMethodName(request.methodName());
            if (request.dataSource() != null) {
                writer.writeData(request.dataSource());
            }
            request.body().writeТо(writer);
            writer.endRequest();
            writer.flush();

            Response response = Response.create()
                    .request(request)
                    .responseBody(createResponseBody(connection.source(), connection))
                    .build();
            success = true;
            return response;
        } finally {
            if (!success) {
                closeQuietly(connection);
            }
            connection = null;
        }
    }

    private ResponseBody createResponseBody(final BufferedSource source, final Connection connection) throws IOException {

        final BytesReader reader = new BytesReader(source) {
            @Override
            public void endObject() throws IOException {
                super.endObject();
                if (currentScope() == SCOPE_RESPONSE){
                    endResponse();
                }
            }
        };

        final long contentLength = reader.beginResponse();

        return new ResponseBody() {

            private ResponseData data;
            private FixedLengthSource dataSource;

            @Override
            public ProtocolReader reader() {
                return reader;
            }

            @Override
            public long contentLength() {
                return contentLength;
            }

            @Override
            public ResponseData data() throws IOException {
                {
                    long dataLength = reader.dataContentLength();
                    if (dataLength == -1) {
                        throw new IOException("Cannot access data content before the response body has been completely read.");
                    } else if (dataLength > 0) {
                        synchronized (reader) {
                            if (data == null) {
                                dataSource = new RecyclingFixedLengthSource(connectionPool, connection, dataLength);
                                data = new ResponseData(Okio.buffer(dataSource), dataLength);
                            }
                        }
                    }
                    return data;
                }
            }

            @Override
            public void close() throws IOException {
                final long dataContentLength;
                final FixedLengthSource dataSource;
                synchronized (reader) {
                    dataContentLength = reader.dataContentLength();
                    dataSource = this.dataSource;
                }
                if (dataContentLength == 0 || dataSource != null && dataSource.bytesRemaining() == 0) {
                    // All possible data has been read, safe to reuse the connection.
                    connectionPool.recycle((RealConnection) connection);
                } else {
                    // It is unknown whether all data from the response has been read, no connection reuse is possible.
                    connection.close();
                }
            }
        };
    }

    @Override
    public void execute(Callback callback) {
        throw new UnsupportedOperationException();
    }

    private RealConnection obtainConnection() throws IOException {
        RealConnection connection;
        while ((connection = (RealConnection) connectionPool.get()) != null) {
            if (connection.isHealthy(eagerlyCheckConnectivity)) {
                return connection;
            } else {
                closeQuietly(connection);
            }
        }

        // No pooled connections available, just build a new one.
        return connectionFactory.openConnection(Endpoint.DEFAULT);
    }

    @Override
    public Request request() {
        return request;
    }


    @Override
    public synchronized boolean isExecuted() {
        return executed;
    }

    @Override
    public void cancel() {
        cancelled = true;
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    private static class RecyclingFixedLengthSource extends FixedLengthSource {

        private ConnectionPool connectionPool;
        private Connection connection;

        private RecyclingFixedLengthSource(ConnectionPool pool, Connection connection, long contentLength) {
            super(connection.source(), contentLength);
            this.connection = connection;
            this.connectionPool = pool;
        }

        @Override
        protected void exhausted(boolean reuseSource) {
            if (reuseSource) {
                connectionPool.recycle((RealConnection) connection);
            } else {
                closeQuietly(connection);
            }
        }
    }
}
