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

import com.pcloud.internal.tls.DefaultHostnameVerifier;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class PCloudAPIClient {

    private final int maxIdleConnections;
    private final int keepAliveDurationMs;
    private int connectTimeoutMs;
    private int writeTimeoutMs;
    private int readTimeoutMs;

    private final SocketFactory socketFactory;
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;
    private final EndpointProvider endpointProvider;
    private final Authenticator authenticator;

    ConnectionPool connectionPool;
    ConnectionFactory connectionFactory;

    private PCloudAPIClient(Builder builder) {
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.writeTimeoutMs = builder.writeTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;

        this.socketFactory = builder.socketFactory != null ? builder.socketFactory : SocketFactory.getDefault();
        this.sslSocketFactory = builder.sslSocketFactory != null ? builder.sslSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();
        this.hostnameVerifier = builder.hostnameVerifier != null ? builder.hostnameVerifier : new DefaultHostnameVerifier();
        this.endpointProvider = builder.endpointProvider != null ? builder.endpointProvider : EndpointProvider.DEFAULT;
        this.authenticator = builder.authenticator != null ? builder.authenticator : new DefaultAuthenticator();

        this.keepAliveDurationMs = builder.keepAliveDurationMs;
        this.maxIdleConnections = builder.maxIdleConnectionCount;

        this.connectionPool = new ConnectionPool(maxIdleConnections, keepAliveDurationMs, MILLISECONDS);
        this.connectionFactory = new ConnectionFactory(socketFactory, sslSocketFactory, hostnameVerifier, connectTimeoutMs, readTimeoutMs, MILLISECONDS);
    }

    public Call newCall(Request request){
        return new RealCall(request, connectionPool, connectionFactory, false);
    }

    public int maxIdleConnections() {
        return maxIdleConnections;
    }

    public int keepAliveDuration() {
        return keepAliveDurationMs;
    }

    public int connectTimeout() {
        return connectTimeoutMs;
    }

    public int writeTimeout() {
        return writeTimeoutMs;
    }

    public int readTimeout() {
        return readTimeoutMs;
    }

    public SocketFactory socketFactory() {
        return socketFactory;
    }

    public SSLSocketFactory sslSocketFactory() {
        return sslSocketFactory;
    }

    public HostnameVerifier hostnameVerifier() {
        return hostnameVerifier;
    }

    public EndpointProvider endpointProvider() {
        return endpointProvider;
    }

    public ConnectionPool connectionPool() {
        return connectionPool;
    }

    public Authenticator authenticator() {
        return authenticator;
    }

    public void shutdown() {
        connectionPool.evictAll();
    }

    public static Builder newClient() {
        return new Builder()
                .setConnectTimeoutMs(15, SECONDS)
                .setWriteTimeoutMs(30, SECONDS)
                .setReadTimeout(30, SECONDS)
                .setConnectionKeepAliveDuration(10, MINUTES)
                .setMaxIdleConnections(5);
    }

    public static class Builder {

        private int connectTimeoutMs;
        private int writeTimeoutMs;
        private int readTimeoutMs;
        private int keepAliveDurationMs;
        private int maxIdleConnectionCount;
        private SocketFactory socketFactory;
        private SSLSocketFactory sslSocketFactory;
        private HostnameVerifier hostnameVerifier;
        private EndpointProvider endpointProvider;
        private Authenticator authenticator;

        private Builder() {
        }

        public Builder setConnectTimeoutMs(int timeout, TimeUnit timeUnit) {
            this.connectTimeoutMs = convertTimeValue(timeout, timeUnit);
            return this;
        }

        public Builder setWriteTimeoutMs(int timeout, TimeUnit timeUnit) {
            this.writeTimeoutMs = convertTimeValue(timeout, timeUnit);
            return this;
        }

        public Builder setReadTimeout(int timeout, TimeUnit timeUnit) {
            this.readTimeoutMs = convertTimeValue(timeout, timeUnit);
            return this;
        }

        public Builder setConnectionKeepAliveDuration(long duration, TimeUnit timeUnit) {
            int durationMs = convertTimeValue(duration, timeUnit);
            if (durationMs == 0) {
                throw new IllegalArgumentException("keepAliveDuration = 0.");
            }
            this.keepAliveDurationMs = durationMs;
            return this;
        }

        public Builder setMaxIdleConnections(int maxIdleConnectionCount) {
            if (maxIdleConnectionCount < 0) {
                throw new IllegalArgumentException("maxIdleConnections < 0: ");
            }
            this.maxIdleConnectionCount = maxIdleConnectionCount;
            return this;
        }

        public Builder setSocketFactory(SocketFactory socketFactory) {
            this.socketFactory = socketFactory;
            return this;
        }

        public Builder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder setEndpointProvider(EndpointProvider endpointProvider) {
            this.endpointProvider = endpointProvider;
            return this;
        }

        public Builder setAuthenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        public PCloudAPIClient create() {
            return new PCloudAPIClient(this);
        }

        private static int convertTimeValue(long timeout, TimeUnit timeUnit) {
            if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
            if (timeUnit == null) throw new NullPointerException("unit == null");
            long millis = timeUnit.toMillis(timeout);
            if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Value too large.");
            if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Value too small.");
            return (int) millis;
        }

    }
}
