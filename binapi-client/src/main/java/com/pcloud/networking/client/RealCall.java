/*
 * Copyright (c) 2017 pCloud AG
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

package com.pcloud.networking.client;

import com.pcloud.networking.protocol.BytesWriter;
import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolRequestWriter;
import com.pcloud.networking.protocol.ProtocolResponseReader;
import com.pcloud.utils.IOUtils;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.pcloud.utils.IOUtils.closeQuietly;

class RealCall implements Call {

    private static final int RESPONSE_LENGTH = 4;

    private Request request;
    private volatile boolean cancelled;
    private volatile boolean executed;

    private Connection connection;

    private ExecutorService callExecutor;
    private ConnectionProvider connectionProvider;
    private List<RequestInterceptor> interceptors;

    RealCall(Request request, ExecutorService callExecutor,
             List<RequestInterceptor> interceptors, ConnectionProvider connectionProvider) {
        this.request = request;
        this.callExecutor = callExecutor;
        this.connectionProvider = connectionProvider;
        this.interceptors = interceptors;
    }

    @Override
    public Response execute() throws IOException {
        checkAndMarkExecuted();
        return getResponse();
    }

    @Override
    public Response enqueueAndWait() throws IOException, InterruptedException {
        checkAndMarkExecuted();
        Response response = null;
        boolean success = false;
        try {
            response = callExecutor.submit(new Callable<Response>() {
                @Override
                public Response call() throws IOException {
                    return getResponse();
                }
            }).get();
            success = true;
            return response;
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        } catch (CancellationException e) {
            throw new IOException(e);
        } finally {
            if (!success) {
                closeQuietly(response);
            }
        }
    }

    @Override
    public Response enqueueAndWait(long timeout, TimeUnit timeUnit)
            throws IOException, InterruptedException, TimeoutException {
        checkAndMarkExecuted();
        Response response = null;
        boolean success = false;
        try {
            response = callExecutor.submit(new Callable<Response>() {
                @Override
                public Response call() throws IOException {
                    return getResponse();
                }
            }).get(timeout, timeUnit);
            success = true;
            return response;
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        } finally {
            if (!success) {
                closeQuietly(response);
            }
        }
    }

    @Override
    public void enqueue(final Callback callback) {
        checkAndMarkExecuted();
        callExecutor.submit(new Runnable() {
            @Override
            public void run() {
                boolean callingCallback = false;
                Response response = null;
                try {
                    response = getResponse();
                    callingCallback = true;
                    callback.onResponse(RealCall.this, response);
                } catch (IOException e) {
                    if (!callingCallback) {
                        callback.onFailure(RealCall.this, e);
                    }
                    closeQuietly(response);
                }
            }
        });
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
        if (!cancelled) {
            cancelled = true;
            closeQuietly(connection);
            connection = null;
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public Call clone() {
        return new RealCall(request, callExecutor, interceptors, connectionProvider);
    }

    private void checkAndMarkExecuted() {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
    }

    private Response getResponse() throws IOException {
        if (cancelled) {
            throw new IOException("Cancelled.");
        }

        Connection connection = request.endpoint() != null ?
                connectionProvider.obtainConnection(request.endpoint()) :
                connectionProvider.obtainConnection();
        this.connection = connection;
        boolean success = false;
        try {
            ProtocolRequestWriter writer = new BytesWriter(connection.sink());
            writer.beginRequest()
                    .writeMethodName(request.methodName());
            if (request.dataSource() != null) {
                writer.writeData(request.dataSource());
            }

            for (RequestInterceptor r : interceptors) {
                r.intercept(request, writer);
            }

            request.body().writeTo(writer);

            writer.endRequest();
            connection.sink().flush();

            Response response = Response.create()
                    .request(request)
                    .responseBody(createResponseBody(connection))
                    .build();
            success = true;
            return response;
        } finally {
            if (!success) {
                closeQuietly(connection);
            }
            this.connection = null;
        }
    }

    private ResponseBody createResponseBody(final Connection connection) throws IOException {
        final long responseLength = IOUtils.peekNumberLe(connection.source(), RESPONSE_LENGTH);

        final FixedLengthSource responseParametersSource = new FixedLengthSource(
                connection.source(), responseLength + RESPONSE_LENGTH) {
            @Override
            protected void exhausted(boolean reuseSource) {
                if (!reuseSource) {
                    connection.close();
                }
            }
        };

        final BufferedSource source = Okio.buffer(responseParametersSource);
        final ProtocolResponseReader reader = new SelfEndingBytesReader(source);
        reader.beginResponse();
        return new ResponseBody() {

            private ResponseData data;
            private FixedLengthSource dataSource;

            @Override
            public ProtocolReader reader() {
                return reader;
            }

            @Override
            public ByteString valuesBytes() throws IOException {
                return source.readByteString();
            }

            @Override
            public long contentLength() {
                return responseLength;
            }

            @Override
            public ResponseData data() throws IOException {
                int scope = reader.currentScope();
                if (scope == ProtocolResponseReader.SCOPE_NONE) {
                    return null;
                } else if (scope != ProtocolResponseReader.SCOPE_DATA) {
                    throw new IOException("Cannot access data content before " +
                            "the response body has been completely read.");
                }
                long dataLength = reader.dataContentLength();
                synchronized (reader) {
                    if (data == null) {
                        dataSource = new RecyclingFixedLengthSource(connectionProvider, connection, dataLength);
                        data = new ResponseData(Okio.buffer(dataSource), dataLength);
                    }
                }
                return data;
            }

            @Override
            public void close() throws IOException {
                final int currentScope;
                final FixedLengthSource dataSource;
                synchronized (reader) {
                    currentScope = reader.currentScope();
                    dataSource = this.dataSource;
                }
                boolean responseReadFully = currentScope == ProtocolResponseReader.SCOPE_NONE;
                if (responseReadFully || (dataSource != null && dataSource.bytesRemaining() == 0L)) {
                    // All possible data has been read, safe to reuse the connection.
                    connectionProvider.recycleConnection(connection);
                } else {
                    // It is unknown whether all data from the response has been read, no connection reuse is possible.
                    connection.close();
                }
            }
        };
    }

    private static class RecyclingFixedLengthSource extends FixedLengthSource {

        private ConnectionProvider connectionPool;
        private Connection connection;

        private RecyclingFixedLengthSource(ConnectionProvider pool,
                                           Connection connection,
                                           long contentLength) throws IOException {
            super(connection.source(), contentLength);
            this.connection = connection;
            this.connectionPool = pool;
        }

        @Override
        protected void exhausted(boolean reuseSource) {
            if (reuseSource) {
                connectionPool.recycleConnection(connection);
            } else {
                closeQuietly(connection);
            }
        }
    }
}
