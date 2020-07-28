/*
 * Copyright (c) 2020 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.networking.client;

import com.pcloud.networking.protocol.DataSource;
import com.pcloud.networking.protocol.ProtocolRequestWriter;
import com.pcloud.networking.protocol.ProtocolResponseReader;
import com.pcloud.networking.protocol.ResponseBytesWriter;
import okio.ByteString;
import okio.Okio;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class RealApiChannelTest {

    private DummyConnection connection;
    private ConnectionProvider connectionProvider;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        connectionProvider = mock(ConnectionProvider.class);
        when(connectionProvider.obtainConnection(any(Endpoint.class)))
                .thenAnswer(new Answer<Connection>() {
                    @Override
                    public Connection answer(InvocationOnMock invocation) throws Throwable {
                        if (connection != null) {
                            throw new IllegalStateException("ConnectionProvider.obtainConnection() called more than one time.");
                        }
                        Endpoint endpoint = (Endpoint) invocation.getArguments()[0];
                        connection = spy(new DummyConnection(endpoint));
                        return connection;
                    }
                });
        when(connectionProvider.obtainConnection()).thenAnswer(new Answer<Connection>() {
            @Override
            public Connection answer(InvocationOnMock invocation) throws Throwable {
                if (connection != null) {
                    throw new IllegalStateException("ConnectionProvider.obtainConnection() called more than one time.");
                }
                Endpoint endpoint = Endpoint.DEFAULT;
                connection = spy(new DummyConnection(endpoint));
                return connection;
            }
        });
    }

    @Test
    public void constructor_Throws_Exceptions_During_Connection_Obtaining() throws Exception {
        Exception connectionError = new IOException("some error");
        when(connectionProvider.obtainConnection(any(Endpoint.class))).thenThrow(connectionError);
        expectedException.expect(is(connectionError));
        createChannelInstance();
    }

    @Test
    public void requested_Connection_Is_For_The_Specified_Endpoint() throws Exception {
        Endpoint endpoint = new Endpoint("somehost", 123);
        try (ApiChannel channel = new RealApiChannel(connectionProvider, endpoint)) {
            verify(connectionProvider).obtainConnection(eq(endpoint));
        }
    }

    @Test
    public void endpoint_Is_Always_Non_Null() throws Exception {
        Endpoint endpoint = new Endpoint("somehost", 12345);
        try (ApiChannel apiChannel = new RealApiChannel(connectionProvider, endpoint)) {
            assertEquals(endpoint, apiChannel.endpoint());
            assertNotNull(apiChannel.endpoint());
            apiChannel.close();
            assertNotNull(apiChannel.endpoint());
        }
    }

    @Test
    public void writer_Is_Always_Non_Null() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            assertNotNull(apiChannel.writer());
            apiChannel.close();
            assertNotNull(apiChannel.writer());
        }
    }

    @Test
    public void reader_Is_Always_Non_Null() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            assertNotNull(apiChannel.reader());
            apiChannel.close();
            assertNotNull(apiChannel.reader());
        }
    }

    @Test
    public void writer_Writes_To_Connection_Sink() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            assertEquals(0, connection.writeBuffer().size());
            apiChannel.writer()
                    .beginRequest()
                    .writeMethodName("somename")
                    .endRequest();
            assertTrue(connection.writeBuffer().size() > 0);
        }
    }

    private ByteString mockResponse() throws IOException {
        return new ResponseBytesWriter()
                .beginObject()
                .writeValue("result", 5000)
                .endObject()
                .bytes();
    }

    private ByteString mockDataResponse() throws IOException {
        return new ResponseBytesWriter()
                .beginObject()
                .setData(ByteString.encodeUtf8("Some data"))
                .endObject()
                .bytes();
    }

    @Test
    public void isOpen_Returns_False_On_Closed_Instance() throws Exception {
        ApiChannel apiChannel = createChannelInstance();
        apiChannel.close();
        assertFalse(apiChannel.isOpen());
    }

    @Test
    public void isOpen_Returns_True_On_Active_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            assertTrue(apiChannel.isOpen());
        }
    }

    @Test
    public void Writer_beginRequest_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class)
                .beginRequest();
    }

    @Test
    public void Writer_writeMethodName_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class)
                .writeMethodName("blah");
    }

    @Test
    public void Writer_writeName_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeName("blah");
    }

    @Test
    public void Writer_writeData_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeData(DataSource.EMPTY);
    }

    @Test
    public void Writer_writeValue_Long_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeValue(1L);
    }

    @Test
    public void Writer_writeValue_String_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeValue("some text");
    }

    @Test
    public void Writer_writeValue_Boolean_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeValue(false);
    }

    @Test
    public void Writer_writeValue_Float_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeValue(1.124f);
    }

    @Test
    public void Writer_writeValue_Double_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeValue(5.124d);
    }

    @Test
    public void Writer_writeValue_Object_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        expectException(initializeAClosedWriter(), ClosedChannelException.class).writeValue(Long.valueOf(1234L));
    }

    @Test
    public void Reader_beginResponse_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());
            apiChannel.close();
            expectException(apiChannel, ClosedChannelException.class)
                    .reader().beginResponse();
        }
    }

    @Test
    public void Reader_endResponse_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        ProtocolResponseReader reader;
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());
            reader = apiChannel.reader();
            reader.beginResponse();
            apiChannel.close();
            expectException(reader, ClosedChannelException.class)
                    .endResponse();
        }
    }

    @Test
    public void Reader_beginArray_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());
            apiChannel.reader().beginResponse();
            apiChannel.close();
            expectException(apiChannel.reader(), ClosedChannelException.class).beginArray();
        }
    }

    @Test
    public void Reader_endArray_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(new ResponseBytesWriter()
                    .beginObject()
                    .writeKey("array")
                    .beginArray()
                    .write("1")
                    .endArray()
                    .endObject()
                    .bytes());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            reader.beginObject();
            reader.readString();
            reader.beginArray();
            apiChannel.close();
            expectException(reader, ClosedChannelException.class).endArray();
        }
    }

    @Test
    public void Reader_beginObject_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());
            apiChannel.reader().beginResponse();
            apiChannel.close();
            expectException(apiChannel.reader(), ClosedChannelException.class).beginObject();
        }
    }

    @Test
    public void Reader_endObject_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            reader.beginObject();
            apiChannel.close();
            expectException(reader, ClosedChannelException.class).endObject();
        }
    }

    @Test
    public void Reader_skipValue_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            apiChannel.close();
            expectException(reader, ClosedChannelException.class).skipValue();
        }
    }

    @Test
    public void Reader_hasNext_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            apiChannel.close();
            expectException(reader, ClosedChannelException.class).hasNext();
        }
    }

    @Test
    public void Reader_peek_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            apiChannel.close();
            expectException(reader, ClosedChannelException.class).peek();
        }
    }


    @Test
    public void Reader_readString_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            reader.beginObject();
            apiChannel.close();
            expectException(apiChannel.reader(), ClosedChannelException.class).readString();
        }
    }

    @Test
    public void Reader_readBoolean_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            reader.beginObject();
            apiChannel.close();
            expectException(apiChannel.reader(), ClosedChannelException.class).readBoolean();
        }
    }

    @Test
    public void Reader_readNumber_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            reader.beginObject();
            apiChannel.close();
            expectException(apiChannel.reader(), ClosedChannelException.class).readNumber();
        }
    }

    @Test
    public void Reader_readData_Throws_ClosedChannelException_On_Closed_Instance() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            connection.readBuffer().write(mockDataResponse());

            ProtocolResponseReader reader = apiChannel.reader();
            reader.beginResponse();
            reader.endResponse();
            apiChannel.close();
            expectException(apiChannel.reader(), ClosedChannelException.class).readData(Okio.buffer(Okio.blackhole()));
        }
    }

    @Test
    public void close_Recycles_Connection_When_No_Requests_Are_Sent() throws Exception {
        ApiChannel apiChannel = createChannelInstance();
        apiChannel.close();
        verifyConnectionRecycled();
    }


    @Test
    public void close_Closes_Connection_When_Responses_Are_Fully_Read() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {

            apiChannel.writer()
                    .beginRequest()
                    .writeMethodName("somename")
                    .endRequest();

            connection.readBuffer().write(mockDataResponse());
            ProtocolResponseReader responseReader = apiChannel.reader();
            responseReader.beginResponse();
            responseReader.endResponse();
            responseReader.readData(Okio.buffer(Okio.blackhole()));

            apiChannel.close();
            verifyConnectionRecycled();
        }
    }

    private void verifyConnectionRecycled() {
        verify(connectionProvider, times(1)).recycleConnection(eq(connection));
        verify(connection, times(0)).close();
    }

    @Test
    public void close_Closes_Connection_With_Pending_Responses() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {
            apiChannel.writer()
                    .beginRequest()
                    .writeMethodName("somename")
                    .endRequest();
        }
        verifyConnectionClosed();
    }

    private void verifyConnectionClosed() {
        verify(connection).close();
        verify(connectionProvider, times(0)).recycleConnection(eq(connection));
    }

    @Test
    public void close_Closes_Connection_With_Unfinished_Response() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {

            apiChannel.writer()
                    .beginRequest()
                    .writeMethodName("somename")
                    .endRequest();

            connection.readBuffer().write(mockResponse());
            ProtocolResponseReader responseReader = apiChannel.reader();
            responseReader.beginResponse();
        }
        verifyConnectionClosed();
    }

    @Test
    public void close_Closes_Connection_With_Finished_Response_And_Unfinished_Data() throws Exception {
        try (ApiChannel apiChannel = createChannelInstance()) {

            apiChannel.writer()
                    .beginRequest()
                    .writeMethodName("somename")
                    .endRequest();

            connection.readBuffer().write(mockDataResponse());
            ProtocolResponseReader responseReader = apiChannel.reader();
            responseReader.beginResponse();
            responseReader.endResponse();
        }
        verifyConnectionClosed();
    }

    private ProtocolRequestWriter initializeAClosedWriter() throws IOException {
        ApiChannel apiChannel = createChannelInstance();
        apiChannel.close();
        return apiChannel.writer();
    }

    private <T> T expectException(T object, Class<? extends Throwable> exceptionType) {
        expectedException.expect(exceptionType);
        return object;
    }


    private RealApiChannel createChannelInstance() throws IOException {
        return new RealApiChannel(connectionProvider, Endpoint.DEFAULT);
    }
}