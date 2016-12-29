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

import com.pcloud.internal.IOUtils;
import okio.*;

import java.io.IOException;
import java.net.ProtocolException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class RealCall implements Call {

    private Request request;
    private volatile boolean cancelled;
    private volatile boolean executed;
    private RealConnection connection;

    private BinaryProtocolCodec binaryProtocolCodec;
    private ConnectionPool connectionPool;
    private ConnectionFactory connectionFactory;
    private boolean eagerlyCheckConnectivity;

    RealCall(Request request, BinaryProtocolCodec binaryProtocolCodec, ConnectionPool connectionPool, ConnectionFactory connectionFactory, boolean eagerlyCheckConnectivity) {
        this.request = request;
        this.binaryProtocolCodec = binaryProtocolCodec;
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

        if (cancelled){
            throw new IOException("Cancelled.");
        }

        connection = obtainConnection();
        boolean success = false;
        try {
            binaryProtocolCodec.writeRequest(connection.getSink(), request);
            Response response = binaryProtocolCodec.readResponse(connection.source());
            if (response.hasBinaryData()) {
                wrapBinaryDataSource(connection, response);
            } else {
                connectionPool.recycle(connection);
            }
            success = true;
            return response;
        } finally {
            if (!success) {
                IOUtils.closeQuietly(connection);
            }
            connection = null;
        }
    }

    @Override
    public void execute(Callback callback) {
        throw new UnsupportedOperationException();
    }

    private Response wrapBinaryDataSource(final RealConnection connection, final Response response) {
        FixedLengthSource source = new FixedLengthSource(response.data().source(), response.data().contentLength()) {
            @Override
            protected void exhausted(boolean reuseSource) {
                if (reuseSource) {
                    connectionPool.recycle(connection);
                } else {
                    IOUtils.closeQuietly(connection);
                }
            }
        };

        return new Response(response.values(), new ResponseData(Okio.buffer(source), source.bytesRemaining));
    }

    private RealConnection obtainConnection() throws IOException {
        RealConnection connection;
        while ((connection = (RealConnection) connectionPool.get()) != null) {
            if (connection.isHealthy(eagerlyCheckConnectivity)) {
                return connection;
            } else {
                IOUtils.closeQuietly(connection);
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
        if (connection != null){
            connection.close();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    private abstract static class FixedLengthSource implements Source {

        int DISCARD_STREAM_TIMEOUT_MILLIS = 200;

        private long bytesRemaining;
        private boolean closed;
        private Source source;
        private ForwardingTimeout timeout;

        FixedLengthSource(Source source, long contentLength) {
            this.source = source;
            this.timeout = new ForwardingTimeout(source.timeout());
            this.bytesRemaining = contentLength;
        }

        public Timeout timeout() {
            return timeout;
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
            if (closed) throw new IllegalStateException("closed");
            if (bytesRemaining == 0) return -1;

            long read = source.read(sink, Math.min(bytesRemaining, byteCount));
            if (read == -1) {
                scrap(false); // The server didn't supply the promised content length.
                throw new ProtocolException("unexpected end of stream");
            }

            bytesRemaining -= read;
            if (bytesRemaining == 0) {
                scrap(true);
            }
            return read;
        }


        @Override
        public synchronized void close() throws IOException {
            if (!closed) {
                if (bytesRemaining != 0 &&
                        !IOUtils.skipAll(this, DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)) {
                    scrap(false);
                }

                closed = true;
            }
        }

        protected abstract void exhausted(boolean reuseSource);

        private void scrap(boolean reuseSource) {
            Timeout oldDelegate = timeout.delegate();
            timeout.setDelegate(Timeout.NONE);
            oldDelegate.clearDeadline();
            oldDelegate.clearTimeout();
            exhausted(reuseSource);
        }
    }
}
