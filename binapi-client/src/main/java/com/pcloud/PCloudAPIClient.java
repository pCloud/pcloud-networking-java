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
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class PCloudAPIClient {

    private static final long DEFAULT_KEEP_ALIVE_TIME_MS = 60;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 15;
    private static final int DEFAULT_READ_TIMEOUT_MS = 30;
    private static final int DEFAULT_WRITE_TIMEOUT_MS = 30;

    private int connectTimeoutMs;
    private int writeTimeoutMs;
    private int readTimeoutMs;

    private final SocketFactory socketFactory;
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;

    private final List<RequestInterceptor> interceptors;

    private ConnectionPool connectionPool;
    private ConnectionFactory connectionFactory;
    private ExecutorService callExecutor;

    private PCloudAPIClient(Builder builder) {
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.writeTimeoutMs = builder.writeTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;

        this.socketFactory = builder.socketFactory != null ? builder.socketFactory : SocketFactory.getDefault();
        this.sslSocketFactory =
                builder.sslSocketFactory != null ?
                        builder.sslSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();
        this.hostnameVerifier =
                builder.hostnameVerifier != null ?
                        builder.hostnameVerifier : DefaultHostnameVerifier.INSTANCE;
        this.connectionPool = builder.connectionPool != null ? builder.connectionPool : new ConnectionPool();
        this.connectionFactory =
                new ConnectionFactory(socketFactory,
                                             sslSocketFactory,
                                             hostnameVerifier,
                                             connectTimeoutMs,
                                             readTimeoutMs,
                                             MILLISECONDS);
        this.callExecutor = builder.callExecutor != null ? builder.callExecutor :
                                    new ThreadPoolExecutor(0,
                                                                  Integer.MAX_VALUE,
                                                                  DEFAULT_KEEP_ALIVE_TIME_MS,
                                                                  TimeUnit.SECONDS,
                                                                  new SynchronousQueue<Runnable>(),
                                                                  //for some reason the formatting below
                                                                  //is an okay indentation for checkstyle...
                                                                  new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "PCloud API Client");
                } });
        this.interceptors = Collections.unmodifiableList(new ArrayList<>(builder.interceptors));
    }

    public Call newCall(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }
        return new RealCall(request,
                                   callExecutor,
                                   interceptors,
                                   new ConnectionProvider(connectionPool, connectionFactory, false));
    }

    public MultiCall newCall(Collection<Request> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("Requests collection cannot be null.");
        }

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Requests collection is empty.");
        }

        Endpoint endpoint = null;
        for (Request request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("Collection cannot contain null requests.");
            }

            if (endpoint != null && !endpoint.equals(request.endpoint())) {
                throw new IllegalArgumentException("Requests from the collection must be opted for the same endpoint.");
            }
            endpoint = request.endpoint();
        }

        return new RealMultiCall(new ArrayList<>(requests),
                                        callExecutor,
                                        interceptors,
                                        new ConnectionProvider(connectionPool, connectionFactory, false));
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

    public ExecutorService callExecutor() {
        return callExecutor;
    }

    public List<RequestInterceptor> interceptors() {
        return interceptors;
    }

    public void shutdown() {
        callExecutor.shutdownNow();
        connectionPool.evictAll();
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static Builder newClient() {
        return new Builder()
                       .setConnectTimeoutMs(DEFAULT_CONNECT_TIMEOUT_MS, SECONDS)
                       .setWriteTimeoutMs(DEFAULT_WRITE_TIMEOUT_MS, SECONDS)
                       .setReadTimeout(DEFAULT_READ_TIMEOUT_MS, SECONDS);
    }

    public static class Builder {

        private int connectTimeoutMs;
        private int writeTimeoutMs;
        private int readTimeoutMs;
        private ConnectionPool connectionPool;
        private SocketFactory socketFactory;
        private SSLSocketFactory sslSocketFactory;
        private HostnameVerifier hostnameVerifier;
        private ExecutorService callExecutor;

        List<RequestInterceptor> interceptors;

        private Builder(PCloudAPIClient cloudAPIClient) {
            this.connectTimeoutMs = cloudAPIClient.connectTimeoutMs;
            this.writeTimeoutMs = cloudAPIClient.writeTimeoutMs;
            this.readTimeoutMs = cloudAPIClient.readTimeoutMs;
            this.connectionPool = cloudAPIClient.connectionPool;
            this.socketFactory = cloudAPIClient.socketFactory;
            this.sslSocketFactory = cloudAPIClient.sslSocketFactory;
            this.hostnameVerifier = cloudAPIClient.hostnameVerifier;
            this.callExecutor = cloudAPIClient.callExecutor;
            this.interceptors = new LinkedList<>(cloudAPIClient.interceptors);
        }

        private Builder() {
            this.interceptors = new LinkedList<>();
        }

        public Builder addInterceptor(RequestInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("RequestInterceptor cannot be null.");
            }
            interceptors.add(interceptor);
            return this;
        }

        public Builder addInterceptors(Collection<RequestInterceptor> interceptors) {
            if (interceptors == null) {
                throw new IllegalArgumentException("RequestInterceptor collection cannot be null.");
            }
            for (RequestInterceptor r : interceptors) {
                addInterceptor(r);
            }
            return this;
        }

        public Builder addInterceptorAtFront(RequestInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("RequestInterceptor cannot be null.");
            }
            interceptors.add(0, interceptor);
            return this;
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

        public Builder connectionPool(ConnectionPool connectionPool) {
            if (connectionPool == null) {
                throw new IllegalArgumentException("ConnectionPool cannot be null.");
            }
            this.connectionPool = connectionPool;
            return this;
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

        public Builder callExecutor(ExecutorService executorService) {
            this.callExecutor = executorService;
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
