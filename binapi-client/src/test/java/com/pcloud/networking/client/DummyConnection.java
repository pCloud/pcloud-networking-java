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


import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by Sajuuk-khar on 13.4.2017 г..
 */
public class DummyConnection implements Connection {

    private Buffer readBuffer;
    private Buffer writeBuffer;
    private Endpoint endpoint;
    private InputStream inputStream;
    private OutputStream outputStream;


    public DummyConnection() {
        this(Endpoint.DEFAULT);
    }

    public DummyConnection(Endpoint endpoint) {
        this.endpoint = endpoint;
        this.readBuffer = new Buffer();
        this.writeBuffer = new Buffer();
        this.inputStream = readBuffer.inputStream();
        this.outputStream = writeBuffer.outputStream();
    }

    public DummyConnection(Endpoint endpoint, byte[] data) {
        this(endpoint);
        readBuffer.write(data);
    }

    public Buffer readBuffer() {
        return readBuffer;
    }

    public Buffer writeBuffer() {
        return writeBuffer;
    }

    @Override
    public InputStream inputStream() {
        return inputStream;
    }

    @Override
    public OutputStream outputStream() {
        return outputStream;
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public BufferedSource source() {
        return readBuffer;
    }

    @Override
    public BufferedSink sink() {
        return writeBuffer;
    }

    @Override
    public void readTimeout(long timeout, TimeUnit timeUnit) throws SocketException {
        readBuffer.timeout().timeout(timeout, timeUnit);
    }

    @Override
    public void writeTimeout(long timeout, TimeUnit timeUnit) {
        writeBuffer.timeout().timeout(timeout, timeUnit);
    }

    @Override
    public int readTimeout() {
        return (int) TimeUnit.NANOSECONDS.toMillis(readBuffer.timeout().timeoutNanos());
    }

    @Override
    public int writeTimeout() {
        return (int) TimeUnit.NANOSECONDS.toMillis(writeBuffer.timeout().timeoutNanos());
    }

    @Override
    public void close() {
        readBuffer.close();
        writeBuffer.close();
    }

    public static Connection withResponses(ResponseBytes... responses) throws IOException {
        return withResponses(Arrays.asList(responses));
    }

    public static  Connection withResponsesFromValues(Collection<Map<String, ?>> responseValues) throws IOException {
        Connection connection = new DummyConnection();
        for (Map<String, ?> values : responseValues) {
            ResponseBytes.from(values).writeTo(connection.source().buffer());
        }
        return connection;
    }

    public static  Connection withResponses(Collection<ResponseBytes> responses) throws IOException {
        Connection connection = new DummyConnection();

        for (ResponseBytes b : responses) {
            b.writeTo(connection.source().buffer());
        }
        return connection;
    }
}
