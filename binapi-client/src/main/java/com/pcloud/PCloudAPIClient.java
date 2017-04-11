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

package com.pcloud;

import com.pcloud.internal.tls.DefaultHostnameVerifier;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class PCloudAPIClient {

    private int connectTimeoutMs;
    private int writeTimeoutMs;
    private int readTimeoutMs;

    private final SocketFactory socketFactory;
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;
    private final Authenticator authenticator;

    private ConnectionPool connectionPool;
    private ConnectionFactory connectionFactory;
    private ExecutorService callExecutor;

    private PCloudAPIClient(Builder builder) {
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.writeTimeoutMs = builder.writeTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;

        this.socketFactory = builder.socketFactory != null ? builder.socketFactory : SocketFactory.getDefault();
        this.sslSocketFactory = builder.sslSocketFactory != null ? builder.sslSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();
        this.hostnameVerifier = builder.hostnameVerifier != null ? builder.hostnameVerifier : DefaultHostnameVerifier.INSTANCE;
        this.authenticator = builder.authenticator != null ? builder.authenticator : new DefaultAuthenticator();
        this.connectionPool = builder.connectionPool != null ? builder.connectionPool : new ConnectionPool();
        this.connectionFactory = new ConnectionFactory(socketFactory, sslSocketFactory, hostnameVerifier, connectTimeoutMs, readTimeoutMs, MILLISECONDS);
        this.callExecutor = builder.callExecutor != null ? builder.callExecutor :
                new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>(), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "PCloud API Client");
                    }
                });
    }

    public Call newCall(Request request){
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }
        return new RealCall(request, callExecutor, new ConnectionProvider(connectionPool, connectionFactory, false));
    }


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

    public ConnectionPool connectionPool() {
        return connectionPool;
    }

    public Authenticator authenticator() {
        return authenticator;
    }

    public ExecutorService callExecutor() {return callExecutor;};

    public void shutdown() {
        connectionPool.evictAll();
    }

    public Builder newBuilder(){
        return new Builder(this);
    }

    public static Builder newClient() {
        return new Builder()
                .setConnectTimeoutMs(15, SECONDS)
                .setWriteTimeoutMs(30, SECONDS)
                .setReadTimeout(30, SECONDS);
    }

    public static class Builder {

        private int connectTimeoutMs;
        private int writeTimeoutMs;
        private int readTimeoutMs;
        private ConnectionPool connectionPool;
        private SocketFactory socketFactory;
        private SSLSocketFactory sslSocketFactory;
        private HostnameVerifier hostnameVerifier;
        private Authenticator authenticator;
        private ExecutorService callExecutor;

        private Builder(PCloudAPIClient cloudAPIClient) {
            this.connectTimeoutMs = cloudAPIClient.connectTimeoutMs;
            this.writeTimeoutMs = cloudAPIClient.writeTimeoutMs;
            this.readTimeoutMs = cloudAPIClient.readTimeoutMs;
            this.connectionPool = cloudAPIClient.connectionPool;
            this.socketFactory = cloudAPIClient.socketFactory;
            this.sslSocketFactory = cloudAPIClient.sslSocketFactory;
            this.hostnameVerifier = cloudAPIClient.hostnameVerifier;
            this.authenticator = cloudAPIClient.authenticator;
            this.callExecutor = cloudAPIClient.callExecutor;
        }

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

        public void connectionPool(ConnectionPool connectionPool) {
            if (connectionPool == null) {
                throw new IllegalArgumentException("ConnectionPool cannot be null.");
            }
            this.connectionPool = connectionPool;
        }

        public Builder setSocketFactory(SocketFactory socketFactory) {
            if (socketFactory == null) {
                throw new IllegalArgumentException("SocketFactory cannot be null.");
            }
            this.socketFactory = socketFactory;
            return this;
        }

        public Builder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
            if (sslSocketFactory == null) {
                throw new IllegalArgumentException("SSLSocketFactory cannot be null.");
            }
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier == null) {
                throw new IllegalArgumentException("HostnameVerifier cannot be null.");
            }
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder setAuthenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        public void callExecutor(ExecutorService executorService) {
            this.callExecutor = executorService;
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
