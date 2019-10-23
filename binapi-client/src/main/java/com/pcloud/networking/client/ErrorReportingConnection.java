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

import okio.Buffer;
import okio.ForwardingSink;
import okio.ForwardingSource;
import okio.Sink;
import okio.Source;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

class ErrorReportingConnection extends RealConnection {

    private EndpointProvider endpointProvider;

    ErrorReportingConnection(
            SocketFactory socketFactory,
            SSLSocketFactory sslSocketFactory,
            HostnameVerifier hostnameVerifier,
            Endpoint endpoint) {
        super(socketFactory, sslSocketFactory, hostnameVerifier, endpoint);
    }

    @Override
    void connect(int connectTimeout, TimeUnit timeUnit) throws IOException {
        try {
            super.connect(connectTimeout, timeUnit);
        } catch (IOException e) {
            EndpointProvider endpointProvider = endpointProvider();
            if (endpointProvider != null) {
                endpointProvider.endpointConnectionError(endpoint(), e);
            }
            throw e;
        }
    }

    void endpointProvider(EndpointProvider endpointProvider) {
        synchronized (this) {
            this.endpointProvider = endpointProvider;
        }
    }

    private EndpointProvider endpointProvider() {
        synchronized (this) {
            return endpointProvider;
        }
    }

    @Override
    protected Source createSource(Socket socket) throws IOException {
        Source source = super.createSource(socket);
        EndpointProvider endpointProvider = endpointProvider();
        if (endpointProvider != null) {
            return new ReportingSource(source, endpointProvider, endpoint());
        } else {
            return source;
        }
    }

    @Override
    protected Sink createSink(Socket socket) throws IOException {
        Sink sink = super.createSink(socket);
        EndpointProvider endpointProvider = endpointProvider();
        if (endpointProvider != null) {
            return new ReportingSink(sink, endpointProvider, endpoint());
        } else {
            return sink;
        }
    }

    static class ReportingSource extends ForwardingSource {
        private final EndpointProvider endpointProvider;
        private final Endpoint endpoint;

        ReportingSource(Source source, EndpointProvider endpointProvider, Endpoint endpoint) {
            super(source);
            this.endpointProvider = endpointProvider;
            this.endpoint = endpoint;
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            try {
                return super.read(sink, byteCount);
            } catch (IOException e) {
                endpointProvider.endpointReadError(endpoint, e);
                throw e;
            }
        }
    }

    static class ReportingSink extends ForwardingSink {
        private final EndpointProvider endpointProvider;
        private final Endpoint endpoint;

        ReportingSink(Sink sink, EndpointProvider endpointProvider, Endpoint endpoint) {
            super(sink);
            this.endpointProvider = endpointProvider;
            this.endpoint = endpoint;
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            try {
                super.write(source, byteCount);
            } catch (IOException e) {
                endpointProvider.endpointWriteError(endpoint, e);
                throw e;
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                super.flush();
            } catch (IOException e) {
                endpointProvider.endpointWriteError(endpoint, e);
                throw e;
            }
        }
    }
}
