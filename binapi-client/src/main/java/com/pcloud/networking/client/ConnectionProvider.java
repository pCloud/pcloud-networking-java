/*
 * Copyright (c) 2017 pCloud AG
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

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.pcloud.utils.IOUtils.closeQuietly;

class ConnectionProvider {

    private ConnectionPool connectionPool;
    private EndpointProvider endpointProvider;
    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;
    private int connectTimeout;
    private int readTimeout;
    private int writeTimeout;
    private boolean eagerlyCheckConnectivity;

    ConnectionProvider(ConnectionPool connectionPool,
                       EndpointProvider endpointProvider,
                       SocketFactory socketFactory,
                       SSLSocketFactory sslSocketFactory,
                       HostnameVerifier hostnameVerifier,
                       int connectTimeout, int readTimeout, int writeTimeout,
                       boolean eagerlyCheckConnectivity) {
        this.connectionPool = connectionPool;
        this.endpointProvider = endpointProvider;
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.eagerlyCheckConnectivity = eagerlyCheckConnectivity;
    }

    Connection obtainConnection() throws IOException {
        return obtainConnection(endpointProvider.endpoint());
    }

    Connection obtainConnection(Endpoint endpoint) throws IOException {
        ErrorReportingConnection result = null;
        RealConnection cachedConnection;
        while ((cachedConnection = connectionPool.get(endpoint)) != null) {
            if (cachedConnection.isHealthy(eagerlyCheckConnectivity)) {
                if (!(cachedConnection instanceof ErrorReportingConnection)) {
                    throw new IllegalStateException("Invalid cached connection type.");
                }
                result = (ErrorReportingConnection) cachedConnection;
                break;
            } else {
                closeQuietly(cachedConnection);
            }
        }

        if (result == null) {
            // No pooled connections available, just build a new one.
            boolean connected = false;
            try {
                result = new ErrorReportingConnection(socketFactory, sslSocketFactory, hostnameVerifier, endpoint);
                result.connect(connectTimeout, TimeUnit.MILLISECONDS);
                result.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
                result.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
                connected = true;
            } finally {
                if (!connected) {
                    closeQuietly(result);
                }
            }
        }
        result.endpointProvider(endpointProvider);
        return result;
    }

    void recycleConnection(Connection connection) {
        if (!(connection instanceof ErrorReportingConnection)) {
            throw new IllegalStateException("Cannot recycle an unknown connection.");
        }
        ((ErrorReportingConnection) connection).endpointProvider(null);
        connectionPool.recycle((RealConnection) connection);
    }
}
