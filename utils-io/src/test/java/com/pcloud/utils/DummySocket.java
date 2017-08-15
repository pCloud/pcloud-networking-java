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

package com.pcloud.utils;

import okio.Buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class DummySocket extends Socket {

    private Buffer input;
    private Buffer output;
    private int soTimeout;
    private boolean connected;
    private boolean closed;

    public DummySocket() {
        this(new Buffer(), new Buffer());
    }

    public DummySocket(Buffer input, Buffer output) {
        this.input = input;
        this.output = output;
    }

    public Buffer input() {
        return input;
    }

    public Buffer output() {
        return output;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return input.inputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return output.outputStream();
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        connected = true;
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        soTimeout = timeout;
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return soTimeout;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
        connected = false;
    }
}
