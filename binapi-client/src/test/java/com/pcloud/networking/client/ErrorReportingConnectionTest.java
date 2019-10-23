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
import okio.ByteString;
import okio.Okio;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class ErrorReportingConnectionTest extends RealConnectionTest {

    private EndpointProvider endpointProvider;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        endpointProvider = mock(EndpointProvider.class);
    }

    @Override
    protected RealConnection createConnection(Endpoint endpoint, int timeoutMs) throws Exception {
        ErrorReportingConnection connection = new ErrorReportingConnection(socketFactory, sslSocketFactory, hostnameVerifier, endpoint);
        connection.endpointProvider(endpointProvider);
        connection.connect(timeoutMs, TimeUnit.MILLISECONDS);
        return spy(connection);
    }

    @Test
    public void connect_Reports_Errors_To_EndpointProvider() throws Exception {
        RealConnection connection = createConnection(Endpoint.DEFAULT);
        UnknownHostException exception = new UnknownHostException();
        doThrow(exception).when(socket).connect(any(SocketAddress.class), anyInt());
        try {
            connection.connect(1, TimeUnit.MINUTES);
        } catch (IOException e) {
            assertSame(e, exception);
        }
        verify(endpointProvider).endpointConnectionError(eq(connection.endpoint()), refEq(exception));
        verifyNoMoreInteractions(endpointProvider);
    }

    @Test
    public void source_Read_Reports_Errors_To_EndpointProvider() throws Exception {
        Connection connection = createConnection(Endpoint.DEFAULT);
        IOException exception = new IOException();
        doThrow(exception).when(osSource)
                .read(any(Buffer.class), anyLong());
        try {
            connection.source().readAll(Okio.blackhole());
        } catch (IOException e) {
            assertSame(e, exception);
        }
        verify(endpointProvider).endpointReadError(eq(connection.endpoint()), refEq(exception));
        verifyNoMoreInteractions(endpointProvider);
    }

    @Test
    public void sink_Write_Reports_Errors_To_EndpointProvider() throws Exception {
        Connection connection = createConnection(Endpoint.DEFAULT);
        IOException exception = new IOException();
        doThrow(exception).when(osSink)
                .write(any(Buffer.class), anyLong());
        try {
            connection.sink().write(ByteString.decodeHex("abcdef"));
            connection.sink().flush();
        } catch (IOException e) {
            assertSame(e, exception);
        }
        verify(endpointProvider).endpointWriteError(eq(connection.endpoint()), refEq(exception));
        verifyNoMoreInteractions(endpointProvider);
    }

    @Test
    public void sink_Flush_Reports_Errors_To_EndpointProvider() throws Exception {
        Connection connection = createConnection(Endpoint.DEFAULT);
        IOException exception = new IOException();
        doThrow(exception).when(osSink).flush();
        try {
            connection.sink().write(ByteString.decodeHex("abcdef"));
            connection.sink().flush();
        } catch (IOException e) {
            assertSame(e, exception);
        }
        verify(endpointProvider).endpointWriteError(eq(connection.endpoint()), refEq(exception));
        verifyNoMoreInteractions(endpointProvider);
    }

}
