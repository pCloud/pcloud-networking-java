package com.pcloud;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by Georgi on 11/8/2016.
 */

public class Endpoint {
    private final String host;
    private final int port;

    public Endpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public SocketAddress socketAddress() {
        return new InetSocketAddress(host, port);
    }
}
