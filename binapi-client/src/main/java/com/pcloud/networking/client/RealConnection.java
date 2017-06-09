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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import static com.pcloud.utils.IOUtils.closeQuietly;
import static com.pcloud.utils.IOUtils.isAndroidGetsocknameError;

class RealConnection implements Connection {

    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;

    private Socket rawSocket;
    private SSLSocket socket;
    private BufferedSource source;
    private BufferedSink sink;

    private long idleAtNanos;
    private Endpoint endpoint;

    RealConnection(SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
    }

    void connect(Endpoint endpoint, int connectTimeout,
                 int readTimeout, TimeUnit timeUnit) throws ConnectException {

        if (socket != null) {
            throw new IllegalStateException("Already connected.");
        }
        boolean success = false;
        try {
            this.rawSocket = createSocket(endpoint, connectTimeout, readTimeout, timeUnit);
            this.socket = upgradeSocket(rawSocket, endpoint, readTimeout, timeUnit);
            this.source = Okio.buffer(Okio.source(socket));
            this.sink = Okio.buffer(Okio.sink(socket));
            this.endpoint = endpoint;
            success = true;
        } catch (IOException e) {
            throw new ConnectException(e);
        } finally {
            if (!success) {
                close();
            }
        }
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public BufferedSource source() {
        return source;
    }

    @Override
    public BufferedSink sink() {
        return sink;
    }

    boolean isHealthy(boolean doExtensiveChecks) {
        if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
            return false;
        }

        if (doExtensiveChecks) {
            try {
                int readTimeout = socket.getSoTimeout();
                try {
                    socket.setSoTimeout(1);
                    return !source.exhausted();
                } finally {
                    socket.setSoTimeout(readTimeout);
                }
            } catch (SocketTimeoutException ignored) {
                ignored.printStackTrace();
                // Read timed out; socket is good.
            } catch (IOException e) {
                return false; // Couldn't read; socket is closed.
            }
        }

        return true;
    }

    void setIdle(long nowNanos) {
        idleAtNanos = nowNanos;
    }

    long idleAtNanos() {
        return idleAtNanos;
    }

    @Override
    public void close() {
        /*
        * Close only the raw socket, all other Closeable
        * properties wrap around it and would get closed too.
        * Also closing the plain socket is non-blocking,
        * while the SSL socket may block as it may be doing
        * some synchronous IO in SSLSocket.close() based on
        * the underlying implementation (namely OpenSSL on Android).
        * */
        closeQuietly(rawSocket);
        rawSocket = null;
        socket = null;
        source = null;
        sink = null;
        endpoint = null;
    }

    private Socket createSocket(Endpoint endpoint, int connectTimeout,
                                int readTimeout, TimeUnit timeUnit) throws IOException {
        Socket socket = null;
        boolean connectionSucceeded = false;
        try {
            socket = socketFactory.createSocket();
            socket.setSoTimeout((int) timeUnit.toMillis(readTimeout));
            socket.connect(endpoint.socketAddress(), (int) timeUnit.toMillis(connectTimeout));
            connectionSucceeded = true;
            return socket;

        } catch (AssertionError e) {
            if (isAndroidGetsocknameError(e)) throw new IOException(e);
            throw e;
        } finally {
            if (!connectionSucceeded) {
                closeQuietly(socket);
            }
        }
    }

    private SSLSocket upgradeSocket(Socket rawSocket, Endpoint endpoint,
                                    int readTimeout, TimeUnit timeUnit) throws IOException {
        SSLSocket socket = null;
        boolean connectionSucceeded = false;
        try {
            socket = (SSLSocket) sslSocketFactory.createSocket(
                    rawSocket,
                    endpoint.host(),
                    endpoint.port(),
                    true /*close wrapped socket*/);

            socket.setSoTimeout((int) timeUnit.toMillis(readTimeout));
            socket.startHandshake();
            if (!hostnameVerifier.verify(endpoint.host(), socket.getSession())) {
                throw new SSLPeerUnverifiedException("Hostname " + endpoint.host() + " not verified:");
            }
            connectionSucceeded = true;
            return socket;

        } catch (AssertionError e) {
            if (isAndroidGetsocknameError(e)) throw new IOException(e);
            throw e;
        } finally {
            if (!connectionSucceeded) {
                closeQuietly(socket);
            }
        }
    }
}
