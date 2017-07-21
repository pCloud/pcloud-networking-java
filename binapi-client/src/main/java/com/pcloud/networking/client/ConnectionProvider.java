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
    private ConnectionFactory connectionFactory;
    private int connectTimeout;
    private int readTimeout;
    private int writeTimeout;
    private boolean eagerlyCheckConnectivity;

    ConnectionProvider(ConnectionPool connectionPool,
                       ConnectionFactory connectionFactory,
                       int connectTimeout, int readTimeout, int writeTimeout,
                       boolean eagerlyCheckConnectivity) {
        this.connectionPool = connectionPool;
        this.connectionFactory = connectionFactory;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.eagerlyCheckConnectivity = eagerlyCheckConnectivity;
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
            connection = connectionFactory.openConnection(endpoint, connectTimeout, TimeUnit.MILLISECONDS);
        }

        connection.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        connection.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
        return connection;
    }

    void recycleConnection(Connection connection) {
        connectionPool.recycle(connection);
    }
}
