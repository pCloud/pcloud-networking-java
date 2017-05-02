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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Implementation to wrap the details of an endpoint - a host address, a port and a {@linkplain SocketAddress}.
 *
 */
@SuppressWarnings("WeakerAccess")
public final class Endpoint {
    /**
     * The default {@linkplain Endpoint} for the binApi client.
     */
    public static final Endpoint DEFAULT = new Endpoint("binapi.pcloud.com", 443);

    private static final int MINIMUM_PORT_NUMBER = 0;
    private static final int MAXIMUM_PORT_NUMBER = 65535;

    private final String host;
    private final int port;

    /**
     * Creates the {@linkplain Endpoint} instance.
     *
     * @param host The host URL
     * @param port The port on which we will connect
     * @throws IllegalArgumentException on a null host parameter and on a port less than 0 or greater than 65535
     */
    public Endpoint(String host, int port) {
        if (host == null) {
            throw new IllegalArgumentException("The host argument cannot be null!");
        }
        if (port < MINIMUM_PORT_NUMBER || port > MAXIMUM_PORT_NUMBER) {
            throw new IllegalArgumentException("The port argument can range from "
                    + MINIMUM_PORT_NUMBER + " to " + MAXIMUM_PORT_NUMBER + " inclusively");
        }
        this.host = host;
        this.port = port;
    }

    /**
     * Returns the host URL which cannot be null.
     *
     * @return The host URL
     */
    public String host() {
        return host;
    }

    /**
     * Returns the port which can range between 0 and 65535
     *
     * @return The port on which we are connecting
     */
    public int port() {
        return port;
    }

    /**
     * Returns the {@linkplain SocketAddress} for this post and port
     *
     * @return The {@linkplain SocketAddress} for this host and port
     */
    public SocketAddress socketAddress() {
        return new InetSocketAddress(host, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Endpoint endpoint = (Endpoint) o;

        if (port != endpoint.port) return false;
        return host.equals(endpoint.host);

    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
