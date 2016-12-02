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

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

class ConnectionFactory {

    private EndpointProvider endpointProvider;
    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;
    private int connectTimeout;
    private int readTimeout;

    ConnectionFactory(PCloudAPIClient cloudAPIClient) {
        this.endpointProvider = cloudAPIClient.endpointProvider();
        this.socketFactory = cloudAPIClient.socketFactory();
        this.sslSocketFactory = cloudAPIClient.sslSocketFactory();
        this.hostnameVerifier = cloudAPIClient.hostnameVerifier();
        this.connectTimeout = cloudAPIClient.connectTimeout();
        this.readTimeout = cloudAPIClient.readTimeout();
    }

    RealConnection openConnection() throws IOException {
        RealConnection connection = new RealConnection(socketFactory, sslSocketFactory, hostnameVerifier);
        boolean connected = false;
        try {
            Endpoint endpoint = endpointProvider.getEndpoint();
            if (endpoint == null) {
                throw new IllegalStateException("EndpointProvider returned a null Endpoint");
            }
            connection.connect(endpoint, connectTimeout, readTimeout, TimeUnit.MILLISECONDS);
            connected = true;
        } finally {
            if (!connected) {
                connection.close();
            }
        }

        return connection;
    }
}
