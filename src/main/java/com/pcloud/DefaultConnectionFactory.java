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
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DefaultConnectionFactory implements ConnectionFactory {

    private Endpoint endpoint = new Endpoint("binapi.pcloud.com", 443);
    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;

    public DefaultConnectionFactory() {
        this(new Endpoint("binapi.pcloud.com", 443),
                SocketFactory.getDefault(),
                (SSLSocketFactory) SSLSocketFactory.getDefault(),
                HttpsURLConnection.getDefaultHostnameVerifier());
    }

    public DefaultConnectionFactory(Endpoint endpoint, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
        this.endpoint = endpoint;
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
    }

    public Connection openConnection() throws IOException {
        Connection connection = new Connection(socketFactory, sslSocketFactory, hostnameVerifier);
        boolean connected = false;
        try {
            connection.connect(endpoint, 30, 60, TimeUnit.SECONDS);
            connected = true;
            return connection;
        } finally {
            if(!connected) {
                connection.close();
            }
        }
    }
}
