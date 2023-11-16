/*
 * Copyright (c) 2020 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.pcloud.networking.client.ResponseBodyUtils.checkNotAlreadyRead;
import static com.pcloud.networking.client.ResponseBodyUtils.skipRemainingValues;
import static com.pcloud.utils.IOUtils.closeQuietly;

class RealCall implements Call {

    private static final int RESPONSE_LENGTH = 4;


    private final Request request;
    private final ExecutorService callExecutor;
    private final ConnectionProvider connectionProvider;
    private final List<RequestInterceptor> interceptors;

    private volatile boolean cancelled;
    private volatile boolean executed;
    private Connection connection;

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
        try {
            return callExecutor.submit(new Callable<Response>() {
                @Override
                public Response call() throws IOException {
                    return getResponse();
                }
            }).get();
        } catch (ExecutionException e) {
            cancel();
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        } catch (CancellationException e) {
            cancel();
            throw new IOException(e);
        }
    }

    @Override
    public Response enqueueAndWait(long timeout, TimeUnit timeUnit)
            throws IOException, InterruptedException, TimeoutException {
        checkAndMarkExecuted();
        boolean success = false;
        try {
            Response response = callExecutor.submit(new Callable<Response>() {
                @Override
                public Response call() throws IOException {
                    return getResponse();
                }
            }).get(timeout, timeUnit);
            success = true;
            return response;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        } finally {
            if (!success) {
                cancel();
            }
        }
    }

    @Override
    public void enqueue(final Callback callback) {
        checkAndMarkExecuted();
        callExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!isCancelled()) {
                    boolean callingCallback = false;
                    Response response = null;
                    boolean success = false;
                    try {
                        response = getResponse();
                        callingCallback = true;
                        callback.onResponse(RealCall.this, response);
                        callingCallback = false;
                        success = true;
                    } catch (IOException e) {
                        if (!callingCallback) {
                            callback.onFailure(RealCall.this, e);
                        }
                    } finally {
                        if (!success) {
                            cancel();
                            closeQuietly(response);
                        }
                    }
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
            Connection connection;
            synchronized (this) {
                connection = this.connection;
                this.connection = null;
            }
            closeQuietly(connection);
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
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
        synchronized (this) {
            this.connection = connection;
        }
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
            synchronized (this) {
                this.connection = null;
            }
        }
    }

    private ResponseBody createResponseBody(final Connection connection) throws IOException {
        final long responseLength = IOUtils.peekNumberLe(connection.source(), RESPONSE_LENGTH);

        final FixedLengthSource responseParametersSource = new AutoCloseSource(connection, responseLength);
        final BufferedSource source = Okio.buffer(responseParametersSource);
        final ProtocolResponseReader reader = new SelfEndingBytesReader(source);

        reader.beginResponse();
        return new ResponseBody() {

            private final Endpoint endpoint = connection.endpoint();
            private ResponseData data;
            private FixedLengthSource dataSource;

            @Override
            public ProtocolReader reader() {
                return reader;
            }

            @Override
            public long contentLength() {
                return responseLength;
            }

            @Override
            public Endpoint endpoint() {
                return endpoint;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                checkNotAlreadyRead(this);
                source.peek().readAll(sink);
                reader.beginObject();
                skipRemainingValues(this);
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
            public void close() {
                final int currentScope;
                final FixedLengthSource dataSource;
                synchronized (reader) {
                    currentScope = reader.currentScope();
                    dataSource = this.dataSource;
                }
                boolean responseReadFully = currentScope == ProtocolResponseReader.SCOPE_NONE;
                if (responseReadFully && (dataSource == null || dataSource.bytesRemaining() == 0L)) {
                    // All possible data has been read, safe to reuse the connection.
                    connectionProvider.recycleConnection(connection);
                } else {
                    // It is unknown whether all data from the response has been read, no connection reuse is possible.
                    closeQuietly(connection);
                }
            }
        };
    }

    private static class RecyclingFixedLengthSource extends FixedLengthSource {

        private final ConnectionProvider connectionPool;
        private final Connection connection;

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

    private static class AutoCloseSource extends FixedLengthSource {
        private final Connection connection;

        AutoCloseSource(Connection connection, long responseLength) throws IOException {
            super(connection.source(), responseLength + RealCall.RESPONSE_LENGTH);
            this.connection = connection;
        }

        @Override
        protected void exhausted(boolean reuseSource) {
            if (!reuseSource) {
                connection.close();
            }
        }
    }
}
