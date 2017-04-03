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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.pcloud.internal.ClientIOUtils;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

class RealConnection implements Connection {

    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;

    private Socket socket;
    private BufferedSource source;
    private BufferedSink sink;

    private long idleAtNanos;

    RealConnection(SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
        this.socketFactory = socketFactory;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
    }

    @Override
    public void connect(Endpoint endpoint, int connectTimeout, int readTimeout, TimeUnit timeUnit) throws com.pcloud.ConnectException{
        if (socket != null) {
            throw new IllegalStateException("Already connected.");
        }
        boolean success = false;
        try {
            socket = createSocket(endpoint, connectTimeout, readTimeout, timeUnit);
            source = Okio.buffer(Okio.source(socket));
            sink = Okio.buffer(Okio.sink(socket));
            success = true;
        } catch (IOException e) {
            throw new ConnectException(e);
        }finally {
            if (!success) {
                close();
            }
        }
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
        ClientIOUtils.closeQuietly(socket);
        ClientIOUtils.closeQuietly(source);
        ClientIOUtils.closeQuietly(sink);
        socket = null;
        source = null;
        sink = null;
    }

    private Socket createSocket(Endpoint endpoint, int connectTimeout, int readTimeout, TimeUnit timeUnit) throws IOException {
        Socket socket = null;
        boolean connectionSucceeded = false;
        try {
            socket = socketFactory.createSocket();
            socket.setSoTimeout((int) timeUnit.toMillis(readTimeout));
            socket.connect(endpoint.socketAddress(), (int) timeUnit.toMillis(connectTimeout));
            socket = sslSocketFactory.createSocket(
                    socket,
                    endpoint.host(),
                    endpoint.port(),
                    true /*close wrapped socket*/);

            SSLSocket sslSocket = (SSLSocket) socket;
            sslSocket.setSoTimeout((int) timeUnit.toMillis(readTimeout));
            sslSocket.startHandshake();
            if (!hostnameVerifier.verify(endpoint.host(), sslSocket.getSession())) {
                throw new SSLPeerUnverifiedException("Hostname " + endpoint.host() + " not verified:");
            }
            connectionSucceeded = true;
            return socket;

        } catch (AssertionError e) {
            if (ClientIOUtils.isAndroidGetsocknameError(e)) throw new IOException(e);
            throw e;
        } finally {
            if (!connectionSucceeded) {
                ClientIOUtils.closeQuietly(socket);
            }
        }
    }
}
