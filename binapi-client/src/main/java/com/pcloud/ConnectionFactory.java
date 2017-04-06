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

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

class ConnectionFactory {

    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;
    private int connectTimeout;
    private int readTimeout;
    private TimeUnit timeUnit;

    ConnectionFactory(SocketFactory socketFactory,
                      SSLSocketFactory sslSocketFactory,
                      HostnameVerifier hostnameVerifier,
                      int connectTimeout, int readTimeout, TimeUnit timeUnit) {
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.timeUnit = timeUnit;
    }

    RealConnection openConnection(Endpoint endpoint) throws IOException {
        if (endpoint == null) {
            throw new AssertionError("null endpoint argument.");
        }

        RealConnection connection = new RealConnection(socketFactory, sslSocketFactory, hostnameVerifier);
        boolean connected = false;
        try {
            connection.connect(endpoint, connectTimeout, readTimeout, timeUnit);
            connected = true;
        } finally {
            if (!connected) {
                connection.close();
            }
        }

        return connection;
    }
}
