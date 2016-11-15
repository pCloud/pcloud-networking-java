package com.pcloud;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Georgi on 11/13/2016.
 */
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
