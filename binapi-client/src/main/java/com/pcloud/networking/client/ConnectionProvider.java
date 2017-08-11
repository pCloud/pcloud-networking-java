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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.pcloud.utils.IOUtils.closeQuietly;

class ConnectionProvider {

    private ConnectionPool connectionPool;
    private EndpointProvider endpointProvider;
    private ConnectionFactory connectionFactory;
    private int connectTimeout;
    private int readTimeout;
    private int writeTimeout;
    private boolean eagerlyCheckConnectivity;

    ConnectionProvider(ConnectionPool connectionPool, EndpointProvider endpointProvider,
                       ConnectionFactory connectionFactory,
                       int connectTimeout, int readTimeout, int writeTimeout,
                       boolean eagerlyCheckConnectivity) {
        this.connectionPool = connectionPool;
        this.endpointProvider = endpointProvider;
        this.connectionFactory = connectionFactory;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.eagerlyCheckConnectivity = eagerlyCheckConnectivity;
    }

    Connection obtainConnection() throws IOException {
        return obtainConnection(endpointProvider.endpoint());
    }

    Connection obtainConnection(Endpoint endpoint) throws IOException {
        Connection connection;
        while ((connection = connectionPool.get(endpoint)) != null) {
            if (((RealConnection) connection).isHealthy(eagerlyCheckConnectivity)) {
                break;
            } else {
                closeQuietly(connection);
                connection = null;
            }
        }

        if (connection == null) {
            // No pooled connections available, just build a new one.
            try {
                connection = connectionFactory.openConnection(endpoint, connectTimeout, TimeUnit.MILLISECONDS);
            } catch (IOException e) {
                endpointProvider.endpointConnectionError(endpoint, e);
                throw e;
            }
        }

        ForwardingConnection forwardingConnection = new ForwardingConnection(connection, endpointProvider);
        forwardingConnection.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        forwardingConnection.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);


        return forwardingConnection;
    }

    void recycleConnection(Connection connection) {
        if (connection instanceof ForwardingConnection) {
            connection = ((ForwardingConnection) connection).getWrappedConnection();
        }
        connectionPool.recycle(connection);
    }
}
