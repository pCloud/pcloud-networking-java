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

import com.pcloud.networking.client.internal.tls.DefaultHostnameVerifier;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * An implementation of a client used to make network calls
 * <p>
 * Used to produce {@linkplain Call} and {@linkplain MultiCall} objects to make network requests to the server
 *
 * @see MultiCall
 * @see Call
 * @see Request
 * @see Response
 * @see MultiResponse
 * @see RequestInterceptor
 */
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

    private final ConnectionPool connectionPool;
    private final ConnectionProvider connectionProvider;
    private final EndpointProvider endpointProvider;
    private final ExecutorService callExecutor;

    private PCloudAPIClient(Builder builder) {
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.writeTimeoutMs = builder.writeTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;

        this.socketFactory = builder.socketFactory != null ? builder.socketFactory : SocketFactory.getDefault();

        this.sslSocketFactory = builder.sslSocketFactory != null ?
                        builder.sslSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();

        this.hostnameVerifier = builder.hostnameVerifier != null ?
                        builder.hostnameVerifier : DefaultHostnameVerifier.INSTANCE;

        this.connectionPool = builder.connectionPool != null ? builder.connectionPool : new ConnectionPool();
        this.endpointProvider = builder.endpointProvider != null ? builder.endpointProvider : EndpointProvider.DEFAULT;

        ConnectionFactory connectionFactory = new ConnectionFactory(socketFactory, sslSocketFactory, hostnameVerifier);
        this.connectionProvider = new ConnectionProvider(connectionPool, endpointProvider, connectionFactory,
                connectTimeoutMs, readTimeoutMs, writeTimeoutMs, false);

        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "PCloud API Client");
            }
        };
        this.callExecutor = builder.callExecutor != null ?
                builder.callExecutor : new ThreadPoolExecutor(0,
                Integer.MAX_VALUE,
                DEFAULT_KEEP_ALIVE_TIME_MS,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                threadFactory);

        this.interceptors = Collections.unmodifiableList(new ArrayList<>(builder.interceptors));
    }


    /**
     * Produces a new instance of a {@linkplain Call} object to make a network call
     *<p>
     * The returned {@linkplain Call} object will send the request to the  {@link Endpoint} returned by
     * {@linkplain Request#endpoint()} or if null, it will send to the endpoint returned from this
     *  {@linkplain PCloudAPIClient} instance's {@linkplain EndpointProvider} at the time of execution.
     *
     * @param request A {@linkplain Request} for this call
     * @return A new instance of a {@linkplain Call} object with the specified {@linkplain Request}
     * @throws IllegalArgumentException on a null {@linkplain Request} argument
     */
    public Call newCall(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }
        return new RealCall(request, callExecutor, interceptors, connectionProvider);
    }

    /**
     * Produces a new instance of a {@linkplain MultiCall} object to make multiple network calls efficiently.
     * <p>
     * The returned {@linkplain MultiCall} instance will batch the provided {@linkplain Request} objects
     * and will send them over a single connection, reducing the round-trips and response times.
     * <p>
     *  The batched calls will be sent to the {@linkplain Endpoint} returned from this
     *  {@linkplain PCloudAPIClient} instance's {@linkplain EndpointProvider} at the time of execution.
     *
     * @param requests A non-null {@linkplain Collection} of {@linkplain Request}
     * @return A new instance of a {@linkplain MultiCall} object with the specified requests
     * @throws IllegalArgumentException on a null {@linkplain Collection},
     *                                  on an empty {@linkplain Collection},
     *                                  on a {@linkplain Collection} containing null elements
     */
    public MultiCall newCall(Collection<Request> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("Requests collection cannot be null.");
        }

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Requests collection is empty.");
        }

        for (Request request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("Collection cannot contain null requests.");
            }
        }

        return new RealMultiCall(new ArrayList<>(requests), callExecutor, interceptors,
                connectionProvider, null);
    }

    /**
     * Produces a new instance of a {@linkplain MultiCall} object to make multiple network calls efficiently.
     * <p>
     * The returned {@linkplain MultiCall} instance will batch the provided {@linkplain Request} objects
     * and will send them over a single connection, reducing the round-trips and response times.
     * <p>
     *  The batched calls will be sent to the provided  {@linkplain Endpoint}.
     *
     * @param requests A non-null {@linkplain Collection} of {@linkplain Request}
     * @param endpoint A target {@linkplain Endpoint} for the {@linkplain MultiCall}
     * @return A new instance of a {@linkplain MultiCall} object with the specified requests
     * @throws IllegalArgumentException on a null {@linkplain Collection},
     *                                  on an empty {@linkplain Collection},
     *                                  on a {@linkplain Collection} containing null elements
     */
    public MultiCall newCall(Collection<Request> requests, Endpoint endpoint) {
        if (requests == null) {
            throw new IllegalArgumentException("Requests collection cannot be null.");
        }

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Requests collection is empty.");
        }

        for (Request request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("Collection cannot contain null requests.");
            }
        }

        return new RealMultiCall(new ArrayList<>(requests), callExecutor, interceptors,
                connectionProvider, endpoint);
    }

    /**
     * Returns the maximum amount of time a connection should take to establish itself in milliseconds as an int
     *
     * @return The maximum amount of time a connection should take to establish itself in milliseconds as an int
     */
    public int connectTimeout() {
        return connectTimeoutMs;
    }

    /**
     * Returns the write timeout in milliseconds
     *
     * @return The write timeout in milliseconds
     */
    public int writeTimeout() {
        return writeTimeoutMs;
    }

    /**
     * Returns the read timeout in milliseconds
     *
     * @return The read timeout in milliseconds
     */
    public int readTimeout() {
        return readTimeoutMs;
    }

    /**
     * Returns the {@linkplain SocketFactory} for of this client
     *
     * @return The {@linkplain SocketFactory} for this client
     */
    public SocketFactory socketFactory() {
        return socketFactory;
    }

    /**
     * Returns the {@linkplain SSLSocketFactory} for this client
     *
     * @return The {@linkplain SSLSocketFactory} for this client
     */
    public SSLSocketFactory sslSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Returns the {@linkplain HostnameVerifier} for this client
     *
     * @return The {@linkplain HostnameVerifier} for this client
     */
    public HostnameVerifier hostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Returns the {@linkplain ConnectionPool} for this client
     *
     * @return The {@linkplain ConnectionPool} for this client
     */
    public ConnectionPool connectionPool() {
        return connectionPool;
    }


    /**
     * Returns the {@linkplain ExecutorService} for this client
     *
     * @return The {@linkplain ExecutorService} for this client
     */
    public ExecutorService callExecutor() {
        return callExecutor;
    }

    /**
     * Returns a {@linkplain List} of {@linkplain RequestInterceptor} for this client
     *
     * @return A {@linkplain List} of {@linkplain RequestInterceptor} for this client
     */
    public List<RequestInterceptor> interceptors() {
        return interceptors;
    }

    /**
     * Shuts down the client clearing up resources such as idle connections
     */
    public void shutdown() {
        callExecutor.shutdownNow();
        connectionPool.evictAll();
    }


    /**
     * Returns a new {@linkplain Builder} to construct the {@linkplain PCloudAPIClient}
     *
     * @return A new {@linkplain Builder} to construct the {@linkplain PCloudAPIClient}
     */
    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Create a new {@linkplain Builder} instance
     * <p>
     * Use this method to start configuring a new {@linkplain PCloudAPIClient} instance.
     *
     * @return a new {@linkplain Builder} instance
     */
    public static Builder newClient() {
        return new Builder()
                .setConnectTimeoutMs(DEFAULT_CONNECT_TIMEOUT_MS, SECONDS)
                .setWriteTimeoutMs(DEFAULT_WRITE_TIMEOUT_MS, SECONDS)
                .setReadTimeout(DEFAULT_READ_TIMEOUT_MS, SECONDS);
    }

    /**
     * Builds instances of the {@linkplain PCloudAPIClient} and allows different properties to be set as needed
     */
    public static class Builder {

        private int connectTimeoutMs;
        private int writeTimeoutMs;
        private int readTimeoutMs;
        private ConnectionPool connectionPool;
        private EndpointProvider endpointProvider;
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
            this.endpointProvider = cloudAPIClient.endpointProvider;
            this.socketFactory = cloudAPIClient.socketFactory;
            this.sslSocketFactory = cloudAPIClient.sslSocketFactory;
            this.hostnameVerifier = cloudAPIClient.hostnameVerifier;
            this.callExecutor = cloudAPIClient.callExecutor;
            this.interceptors = new LinkedList<>(cloudAPIClient.interceptors);
        }

        private Builder() {
            this.interceptors = new LinkedList<>();
        }

        /**
         * Adds a new {@linkplain RequestInterceptor}
         *
         * @param interceptor A new {@linkplain RequestInterceptor} to be added to the client
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain RequestInterceptor} argument
         */
        public Builder addInterceptor(RequestInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("RequestInterceptor cannot be null.");
            }
            interceptors.add(interceptor);
            return this;
        }

        /**
         * The same as {@linkplain #addInterceptor(RequestInterceptor)}
         * but allows adding an entire {@linkplain List} of {@linkplain RequestInterceptor}
         *
         * @param interceptors A {@linkplain List} of {@linkplain RequestInterceptor} to be added to the client
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain Collection} or one containing null elements
         */
        public Builder addInterceptors(Collection<RequestInterceptor> interceptors) {
            if (interceptors == null) {
                throw new IllegalArgumentException("RequestInterceptor collection cannot be null.");
            }
            for (RequestInterceptor r : interceptors) {
                addInterceptor(r);
            }
            return this;
        }

        /**
         * Adds a {@linkplain RequestInterceptor} to the front of the {@linkplain List} of interceptors
         *
         * @param interceptor A {@linkplain RequestInterceptor} to be added to the front of the {@linkplain List}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain RequestInterceptor}
         */
        public Builder addInterceptorAtFront(RequestInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("RequestInterceptor cannot be null.");
            }
            interceptors.add(0, interceptor);
            return this;
        }

        /**
         * Sets the connection timeout duration
         *
         * @param timeout  The maximum amount of time a connection should take to establish itself
         * @param timeUnit The unit in which you provided the duration argument
         * @return A reference to the {@linkplain Builder}
         * @throws IllegalArgumentException on a null {@linkplain TimeUnit},
         *                                  0 or a negative value for the duration argument
         *                                  or a duration argument greater than {@linkplain Integer#MAX_VALUE}
         */
        public Builder setConnectTimeoutMs(int timeout, TimeUnit timeUnit) {
            this.connectTimeoutMs = convertTimeValue(timeout, timeUnit);
            return this;
        }

        /**
         * Sets the write timeout duration
         *
         * @param timeout  The duration for the write timeout
         * @param timeUnit The unit in which you specified the duration argument
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain TimeUnit},
         *                                  0 or a negative value for the duration argument
         *                                  or a duration argument greater than {@linkplain Integer#MAX_VALUE}
         */
        public Builder setWriteTimeoutMs(int timeout, TimeUnit timeUnit) {
            this.writeTimeoutMs = convertTimeValue(timeout, timeUnit);
            return this;
        }

        /**
         * Sets the read timeout duration
         *
         * @param timeout  The duration for the read timeout
         * @param timeUnit The unit in which specified the duration argument
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain TimeUnit},
         *                                  0 or a negative value for the duration argument
         *                                  or a duration argument greater than {@linkplain Integer#MAX_VALUE}
         */
        public Builder setReadTimeout(int timeout, TimeUnit timeUnit) {
            this.readTimeoutMs = convertTimeValue(timeout, timeUnit);
            return this;
        }

        /**
         * Sets a {@linkplain ConnectionPool} for the client
         *
         * @param connectionPool A {@linkplain ConnectionPool} to be set to the client
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain ConnectionPool} argument
         */
        public Builder connectionPool(ConnectionPool connectionPool) {
            if (connectionPool == null) {
                throw new IllegalArgumentException("ConnectionPool cannot be null.");
            }
            this.connectionPool = connectionPool;
            return this;
        }

        /**
         * Sets a {@linkplain EndpointProvider} for the client
         *
         * @param endpointProvider A {@linkplain EndpointProvider} to be set to the client
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain EndpointProvider} argument
         */
        public Builder endpointProvider(EndpointProvider endpointProvider) {
            if (endpointProvider == null) {
                throw new IllegalArgumentException("EndpointProvider cannot be null.");
            }
            this.endpointProvider = endpointProvider;
            return this;
        }

        /**
         * Sets the {@linkplain SocketFactory} for the client
         *
         * @param socketFactory The {@linkplain SocketFactory} to be set to the client
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain SocketFactory} argument
         */
        public Builder setSocketFactory(SocketFactory socketFactory) {
            if (socketFactory == null) {
                throw new IllegalArgumentException("SocketFactory cannot be null.");
            }
            this.socketFactory = socketFactory;
            return this;
        }

        /**
         * Sets the {@linkplain SSLSocketFactory} for the client
         *
         * @param sslSocketFactory The {@linkplain SSLSocketFactory} to be set to the client
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain SSLSocketFactory} argument
         */
        public Builder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
            if (sslSocketFactory == null) {
                throw new IllegalArgumentException("SSLSocketFactory cannot be null.");
            }
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        /**
         * Sets the {@linkplain HostnameVerifier} for the client
         *
         * @param hostnameVerifier The {@linkplain HostnameVerifier} to be set to the client
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain HostnameVerifier} argument
         */
        public Builder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier == null) {
                throw new IllegalArgumentException("HostnameVerifier cannot be null.");
            }
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        /**
         * Sets a {@linkplain ExecutorService} for the client
         * <p>
         * If no executor is provided or this method is called with a null argument a default {@linkplain ExecutorService} will be set
         *
         * @param executorService The {@linkplain ExecutorService} to be set to the client
         * @return A reference to the {@linkplain Builder} object
         */
        public Builder callExecutor(ExecutorService executorService) {
            this.callExecutor = executorService;
            return this;
        }

        /**
         * Creates the {@linkplain PCloudAPIClient} with all the parameters set with the {@linkplain Builder}
         *
         * @return A new instance of the {@linkplain PCloudAPIClient} with the parameters set via the {@linkplain Builder}
         */
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
