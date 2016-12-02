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

import okio.*;

import java.io.IOException;
import java.net.ProtocolException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class CommandExecutor {

    private BinaryProtocolCodec binaryProtocolCodec;
    private ConnectionPool connectionPool;
    private ConnectionFactory connectionFactory;
    private Authenticator authenticator;
    private boolean eagerlyCheckConnectivity;

    CommandExecutor(PCloudAPIClient apiClient, boolean eagerlyCheckConnectivity) {
        this.binaryProtocolCodec = new BinaryProtocolCodec();
        this.connectionPool = apiClient.connectionPool();
        this.connectionFactory = new ConnectionFactory(apiClient);
        this.authenticator = apiClient.authenticator();
        this.eagerlyCheckConnectivity = eagerlyCheckConnectivity;
    }

    Response execute(Request request) throws IOException {
        final RealConnection connection = obtainConnection();
        System.out.println("Making request using connection " + connection);
        Response response = execute(request, connection);
        if (response.resultCode() == 1000 || response.resultCode() == 2000) {
            request = authenticator.authenticate(request);
            response = execute(request, connection);
        }
        return response;
    }

    private Response execute(Request request, RealConnection connection) throws IOException {
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
        }
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
        return connectionFactory.openConnection();
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
