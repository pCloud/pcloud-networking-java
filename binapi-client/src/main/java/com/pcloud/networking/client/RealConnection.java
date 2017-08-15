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

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static com.pcloud.utils.IOUtils.closeQuietly;
import static com.pcloud.utils.IOUtils.isAndroidGetsocknameError;

class RealConnection extends BaseConnection {

    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;

    private Socket rawSocket;
    private SSLSocket socket;
    private BufferedSource source;
    private BufferedSink sink;
    private InputStream inputStream;
    private OutputStream outputStream;

    private long idleAtNanos;
    private Endpoint endpoint;
    private boolean connected;

    RealConnection(SocketFactory socketFactory,
                   SSLSocketFactory sslSocketFactory,
                   HostnameVerifier hostnameVerifier,
                   Endpoint endpoint, int connectTimeout, TimeUnit timeUnit) throws IOException {
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.endpoint = endpoint;
        try {
            this.rawSocket = createSocket(endpoint, (int) timeUnit.toMillis(connectTimeout));
            this.socket = upgradeSocket(rawSocket, endpoint);
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            this.source = Okio.buffer(Okio.source(inputStream));
            this.sink = Okio.buffer(Okio.sink(outputStream));
            socket.setSoTimeout(0);
            source.timeout().timeout(readTimeout(), TimeUnit.MILLISECONDS);
            sink.timeout().timeout(writeTimeout(), TimeUnit.MILLISECONDS);
            connected = true;
        } finally {
            if (!connected) {
                close();
            }
        }
    }

    @Override
    public InputStream inputStream() throws IOException {
        synchronized (this) {
            checkNotClosed();
            return inputStream;
        }
    }

    @Override
    public OutputStream outputStream() throws IOException {
        synchronized (this) {
            checkNotClosed();
            return outputStream;
        }
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public BufferedSource source() throws IOException {
        synchronized (this) {
            checkNotClosed();
            return source;
        }
    }

    @Override
    public BufferedSink sink() throws IOException {
        synchronized (this) {
            checkNotClosed();
            return sink;
        }
    }

    boolean isHealthy(boolean doExtensiveChecks) {
        if (!connected) {
            return false;
        }

        Socket socket;
        BufferedSource source;
        synchronized (this) {
            socket = this.socket;
            source = this.source;
        }
        if (socket == null ||
                source == null ||
                socket.isClosed() ||
                socket.isInputShutdown() ||
                socket.isOutputShutdown()) {
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
        synchronized (this) {
            idleAtNanos = nowNanos;
        }
    }

    long idleAtNanos() {
        synchronized (this) {
            return idleAtNanos;
        }
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
        synchronized (this) {
            connected = false;
            closeQuietly(rawSocket);
            inputStream = null;
            outputStream = null;
            rawSocket = null;
            socket = null;
            source = null;
            sink = null;
            endpoint = null;
        }
    }

    private Socket createSocket(Endpoint endpoint, int connectTimeout) throws IOException {
        Socket socket = null;
        boolean connectionSucceeded = false;
        try {
            socket = socketFactory.createSocket();
            socket.connect(endpoint.socketAddress(), connectTimeout);
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

    private SSLSocket upgradeSocket(Socket rawSocket, Endpoint endpoint) throws IOException {
        SSLSocket socket = null;
        boolean connectionSucceeded = false;
        try {
            socket = (SSLSocket) sslSocketFactory.createSocket(
                    rawSocket,
                    endpoint.host(),
                    endpoint.port(),
                    true /*close wrapped socket*/);

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

    private void checkNotClosed() throws IOException {
        if (!connected) {
            throw new IOException("Connection is closed.");
        }
    }
}
