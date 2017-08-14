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
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import okio.Source;
import okio.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dimitard on 9.8.2017 г..
 */
public class ForwardingConnection extends BaseConnection {

    private Connection wrappedConnection;
    private BufferedSource source;
    private BufferedSink sink;

    ForwardingConnection(Connection wrappedConnection, EndpointProvider endpointProvider) {
        this.wrappedConnection = wrappedConnection;
        this.source = Okio.buffer(new ReportingSource(Okio.source(inputStream()), endpointProvider, endpoint()));
        this.sink = Okio.buffer(new ReportingSink(Okio.sink(outputStream()), endpointProvider, endpoint()));
        source.timeout().timeout(wrappedConnection.readTimeout(), TimeUnit.MILLISECONDS);
        sink.timeout().timeout(wrappedConnection.writeTimeout(), TimeUnit.MILLISECONDS);
    }

    Connection getWrappedConnection() {
        return wrappedConnection;
    }

    @Override
    public InputStream inputStream() {
        return wrappedConnection.inputStream();
    }

    @Override
    public OutputStream outputStream() {
        return wrappedConnection.outputStream();
    }

    @Override
    public Endpoint endpoint() {
        return wrappedConnection.endpoint();
    }

    @Override
    public BufferedSource source() {
        return source;
    }

    @Override
    public BufferedSink sink() {
        return sink;
    }

    @Override
    public void close() {
        wrappedConnection.close();
    }

    static class ReportingSource implements Source {
        private Source source;
        private EndpointProvider endpointProvider;
        private Endpoint endpoint;

        ReportingSource(Source source, EndpointProvider endpointProvider, Endpoint endpoint) {
            this.source = source;
            this.endpointProvider = endpointProvider;
            this.endpoint = endpoint;
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            try {
                return source.read(sink, byteCount);
            } catch (IOException e) {
                endpointProvider.endpointConnectionError(endpoint, e);
                throw e;
            }
        }

        @Override
        public Timeout timeout() {
            return source.timeout();
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }

    static class ReportingSink implements Sink {
        private Sink sink;
        private EndpointProvider endpointProvider;
        private Endpoint endpoint;

        ReportingSink(Sink sink, EndpointProvider endpointProvider, Endpoint endpoint) {
            this.sink = sink;
            this.endpointProvider = endpointProvider;
            this.endpoint = endpoint;
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            try {
                sink.write(source, byteCount);
            } catch (IOException e) {
                endpointProvider.endpointConnectionError(endpoint, e);
                throw e;
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                sink.flush();
            } catch (IOException e) {
                endpointProvider.endpointConnectionError(endpoint, e);
                throw e;
            }
        }

        @Override
        public Timeout timeout() {
            return sink.timeout();
        }

        @Override
        public void close() throws IOException {
            sink.close();
        }
    }
}
