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

import com.pcloud.utils.DummyBufferedSink;
import com.pcloud.utils.DummyBufferedSource;
import com.pcloud.utils.DummySocket;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import okio.Sink;
import okio.Source;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RealConnectionTest {

    private static final Endpoint TEST_ENDPOINT = new Endpoint("somehost.api.com", 123);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected SocketFactory socketFactory;
    protected SSLSocketFactory sslSocketFactory;
    protected HostnameVerifier hostnameVerifier;
    protected SSLSocket sslSocket;
    protected DummySocket socket;
    protected BufferedSource osSource;
    protected BufferedSink osSink;

    @Before
    public void setUp() throws Exception {
        osSource = spy(new DummyBufferedSource());
        osSink = spy(new DummyBufferedSink());
        DummySocket dummySocket = new DummySocket(osSource, osSink);
        socket = spy(dummySocket);
        socketFactory = mock(SocketFactory.class);
        when(socketFactory.createSocket()).thenReturn(socket);

        sslSocket = mock(SSLSocket.class);
        when(sslSocket.getInputStream()).thenReturn(dummySocket.getInputStream());
        when(sslSocket.getOutputStream()).thenReturn(dummySocket.getOutputStream());

        sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslSocketFactory.createSocket(any(Socket.class), anyString(), anyInt(), anyBoolean()))
                .thenReturn(sslSocket);

        hostnameVerifier = mock(HostnameVerifier.class);
        when(hostnameVerifier.verify(anyString(), any(SSLSession.class))).thenReturn(true);
    }

    @Test
    public void socketOpenedToCorrectEndpoint() throws Exception {
        final int timeout = 30;

        createConnection(TEST_ENDPOINT, timeout);

        verify(socketFactory).createSocket();
        verify(socket).connect(eq(TEST_ENDPOINT.socketAddress()), eq(timeout));
    }

    @Test
    public void openedSocketWrappedWithSSLSocket() throws Exception {
        createConnection(TEST_ENDPOINT);

        verify(sslSocketFactory).createSocket(
                eq(socket),
                eq(TEST_ENDPOINT.host()),
                eq(TEST_ENDPOINT.port()),
                eq(true));
        verify(sslSocket).startHandshake();
    }

    @Test
    public void wrappedWithSSLSocket_Throws_SSLPeerUnverifiedException_If_HostnameVerifier_Returns_false() throws Exception {
        when(hostnameVerifier.verify(anyString(), any(SSLSession.class))).thenReturn(false);
        expectedException.expect(SSLPeerUnverifiedException.class);
        createConnection();
    }

    @Test
    public void inputStream_Throws_IOException_If_Connection_Closed() throws Exception {
        RealConnection connection = createConnection();
        connection.close();
        expectedException.expect(IOException.class);
        connection.inputStream();
    }

    @Test
    public void outputStream_Throws_IOException_If_Connection_Closed() throws Exception {
        RealConnection connection = createConnection();
        connection.close();
        expectedException.expect(IOException.class);
        connection.outputStream();
    }

    @Test
    public void sink_Throws_IOException_If_Connection_Closed() throws Exception {
        RealConnection connection = createConnection();
        connection.close();
        expectedException.expect(IOException.class);
        connection.sink();
    }

    @Test
    public void source_Throws_IOException_If_Connection_Closed() throws Exception {
        RealConnection connection = createConnection();
        connection.close();
        expectedException.expect(IOException.class);
        connection.source();
    }

    @Test
    public void close_Calls_Close_On_Raw_Socket() throws Exception {
        RealConnection connection = createConnection();
        connection.close();
        verify(socket, atLeastOnce()).close();
    }

    @Test
    public void set_ReadTimeout_Throws_IOException_If_Connection_Closed() throws Exception {
        RealConnection connection = createConnection();
        connection.close();
        expectedException.expect(IOException.class);
        connection.readTimeout(1L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void set_WriteTimeout_Throws_IOException_If_Connection_Closed() throws Exception {
        RealConnection connection = createConnection();
        connection.close();
        expectedException.expect(IOException.class);
        connection.writeTimeout(1L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void endpoint_Returns_Endpoint_Provided_From_Constructor() throws Exception {
        Connection connection = createConnection(TEST_ENDPOINT);
        assertEquals(TEST_ENDPOINT, connection.endpoint());
    }

    @Test
    public void sink_Writes_To_SSLSocket_OutputStream() throws Exception {
        Buffer buffer = new Buffer();
        when(sslSocket.getOutputStream()).thenReturn(buffer.outputStream());

        Connection connection = createConnection();
        ByteString data = ByteString.encodeUtf8("Connections, Connections...");
        connection.sink().write(data).flush();

        assertEquals(data, buffer.readByteString());
    }

    @Test
    public void source_Reads_From_SSLSocket_InputStream() throws Exception {
        Buffer output = new Buffer();
        when(sslSocket.getInputStream()).thenReturn(output.inputStream());

        Connection connection = createConnection();
        ByteString data = ByteString.encodeUtf8("Connections, Connections...");
        output.write(data).flush();

        assertEquals(data, connection.source().readByteString());
    }

    @Test(timeout = 3000)
    public void source_Read_Throws_If_Read_Timeout_Passes() throws Exception {
        Connection connection = createConnection();
        connection.readTimeout(1, TimeUnit.MILLISECONDS);
        Sink source = mock(Sink.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                while (Thread.currentThread().isInterrupted()){
                    //Block
                }
                return null;
            }
        }).when(source).write(any(Buffer.class), anyLong());

        try {
            connection.source().readAll(Okio.blackhole());
        } catch (InterruptedIOException ignored) {
        }
    }

    @Test(timeout = 3000)
    public void sink_Write_Throws_If_Write_Timeout_Passes() throws Exception {
        Connection connection = createConnection();
        connection.writeTimeout(1, TimeUnit.MILLISECONDS);
        Source source = mock(Source.class);
        when(source.read(any(Buffer.class), anyLong()))
                .then(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                while (Thread.currentThread().isInterrupted()){
                    //Block
                }
                return -1L;
            }
        });
        try {
            connection.sink().writeAll(source);
        } catch (InterruptedIOException ignored) {
        }
    }

    protected RealConnection createConnection(Endpoint endpoint, int timeoutMs) throws Exception {
        RealConnection connection = new RealConnection(socketFactory, sslSocketFactory, hostnameVerifier, endpoint);
        connection.connect(timeoutMs, TimeUnit.MILLISECONDS);
        return spy(connection);
    }

    protected RealConnection createConnection(Endpoint endpoint) throws Exception {
        return createConnection(endpoint, Integer.MAX_VALUE);
    }

    protected RealConnection createConnection() throws Exception {
        return createConnection(TEST_ENDPOINT, Integer.MAX_VALUE);
    }
}