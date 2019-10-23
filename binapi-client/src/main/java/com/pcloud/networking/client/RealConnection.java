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
import okio.Sink;
import okio.Source;

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

class RealConnection implements Connection {

    private static final boolean RUNNING_ON_ANDROID;
    private static final int VERSION_INT_OREO = 26;

    static {
        boolean isAndroid = false;
        try {
            Class.forName("android.os.Build");
            isAndroid = true;
        } catch (ClassNotFoundException e) {
            // Not running on Android
        } finally {
            RUNNING_ON_ANDROID = isAndroid;
        }
    }

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
    private volatile boolean connected;
    private volatile boolean closed;

    private int readTimeout = NO_TIMEOUT;
    private int writeTimeout = NO_TIMEOUT;

    RealConnection(SocketFactory socketFactory,
                   SSLSocketFactory sslSocketFactory,
                   HostnameVerifier hostnameVerifier,
                   Endpoint endpoint) {
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.endpoint = endpoint;
    }

    void connect(int connectTimeout, TimeUnit timeUnit) throws IOException {
        checkNotClosed();
        Socket rawSocket = null;
        SSLSocket socket = null;
        boolean connected = false;
        try {
            rawSocket = createSocket(endpoint, (int) timeUnit.toMillis(connectTimeout));
            socket = upgradeSocket(rawSocket, endpoint);
            socket.setSoTimeout(0);
            connected = true;
        } finally {
            if (!connected) {
                closeQuietly(rawSocket);
                closeQuietly(socket);
            }
        }

        synchronized (this) {
            if (closed) {
                closeQuietly(rawSocket);
                closeQuietly(socket);
            } else {
                this.rawSocket = rawSocket;
                this.socket = socket;
                this.source = Okio.buffer(createSource(socket));
                this.sink = Okio.buffer(createSink(socket));
                this.inputStream = source.inputStream();
                this.outputStream = sink.outputStream();
                this.connected = true;
                readTimeout(readTimeout(), TimeUnit.MILLISECONDS);
                writeTimeout(writeTimeout(), TimeUnit.MILLISECONDS);
            }
        }
    }

    protected Sink createSink(Socket socket) throws IOException {
        return Okio.sink(socket);
    }

    protected Source createSource(Socket socket) throws IOException {
        return Okio.source(socket);
    }

    @Override
    public InputStream inputStream() throws IOException {
        checkConnected();
        synchronized (this) {
            checkConnected();
            return inputStream;
        }
    }

    @Override
    public OutputStream outputStream() throws IOException {
        checkConnected();
        synchronized (this) {
            checkConnected();
            return outputStream;
        }
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public BufferedSource source() throws IOException {
        checkConnected();
        synchronized (this) {
            checkConnected();
            return source;
        }
    }

    @Override
    public BufferedSink sink() throws IOException {
        checkConnected();
        synchronized (this) {
            checkConnected();
            return sink;
        }
    }

    boolean isHealthy(boolean doExtensiveChecks) {
        Socket socket;
        BufferedSource source;

        if (!connected || closed) {
            return false;
        }
        synchronized (this) {
            if (!connected) {
                return false;
            } else {
                socket = this.socket;
                source = this.source;
            }
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
                checkNotClosed();
                int readTimeout = socket.getSoTimeout();
                try {
                    socket.setSoTimeout(1);
                    return !source.exhausted();
                } finally {
                    socket.setSoTimeout(readTimeout);
                }
            } catch (SocketTimeoutException ignored) {
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
    public void readTimeout(long timeout, TimeUnit timeUnit) throws IOException {
        synchronized (this) {
            readTimeout = (int) timeUnit.toMillis(timeout);
            BufferedSource source = source();
            if (source != null) {
                source.timeout().timeout(readTimeout, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void writeTimeout(long timeout, TimeUnit timeUnit) throws IOException {
        synchronized (this) {
            writeTimeout = (int) timeUnit.toMillis(timeout);
            BufferedSink sink = sink();
            if (sink != null) {
                sink.timeout().timeout(writeTimeout, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public int readTimeout() {
        synchronized (this) {
            return readTimeout;
        }
    }

    @Override
    public int writeTimeout() {
        synchronized (this) {
            return writeTimeout;
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
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    connected = false;
                    closeQuietly(rawSocket);
                    inputStream = null;
                    outputStream = null;
                    rawSocket = null;
                    socket = null;
                    source = null;
                    sink = null;
                    endpoint = null;
                    closed = true;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Connection(\"" + endpoint() + "\")";
    }

    private Socket createSocket(Endpoint endpoint, int connectTimeout) throws IOException {
        Socket socket = null;
        boolean connectionSucceeded = false;
        try {
            socket = socketFactory.createSocket();
            if (RUNNING_ON_ANDROID) {
                connectSocketAndroid(socket, endpoint, connectTimeout);
            } else {
                connectSocketJava(socket, endpoint, connectTimeout);
            }
            connectionSucceeded = true;
            return socket;
        } finally {
            if (!connectionSucceeded) {
                closeQuietly(socket);
            }
        }
    }

    private void connectSocketJava(Socket socket, Endpoint endpoint, int connectTimeout) throws IOException {
        socket.setSoTimeout(readTimeout());
        socket.connect(endpoint.socketAddress(), connectTimeout);
    }

    private void connectSocketAndroid(Socket socket, Endpoint endpoint, int connectTimeout) throws IOException {
        try {
            socket.setSoTimeout(readTimeout());
            socket.connect(endpoint.socketAddress(), connectTimeout);
        } catch (AssertionError e) {
            if (isAndroidGetsocknameError(e)) {
                throw new IOException(e);
            } else {
                throw e;
            }
        } catch (ClassCastException e) {
            // Add a fix for https://issuetracker.google.com/issues/63649622
            if (android.os.Build.VERSION.SDK_INT == VERSION_INT_OREO) {
                throw new IOException("Exception in connect", e);
            } else {
                throw e;
            }
        }
    }

    private SSLSocket upgradeSocket(Socket rawSocket, Endpoint endpoint) throws IOException {
        SSLSocket socket = null;
        boolean connectionSucceeded = false;
        try {
            Socket newSocket = sslSocketFactory.createSocket(
                    rawSocket,
                    endpoint.host(),
                    endpoint.port(),
                    true /*close wrapped socket*/);
            if (!(newSocket instanceof SSLSocket)) {
                closeQuietly(newSocket);
                throw new IllegalStateException("The SSLSocketFactory did not return a SSLSocket. ");
            }
            socket = (SSLSocket) newSocket;

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

    private void checkConnected() throws IOException {
        if (!connected) {
            throw new IOException("Connection is not connected.");
        }
    }

    private void checkNotClosed() throws IOException {
        if (closed) {
            throw new IOException("Connection is closed.");
        }
    }
}
