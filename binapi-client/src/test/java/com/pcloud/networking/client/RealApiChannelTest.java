package com.pcloud.networking.client;

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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        ApiChannel apiChannel = new RealApiChannel(connectionProvider, Endpoint.DEFAULT);
    }

    @Test
    public void requested_Connection_Is_For_The_Specified_Endpoint() throws Exception {
        Endpoint endpoint = new Endpoint("somehost", 123);
        ApiChannel apiChannel = new RealApiChannel(connectionProvider, endpoint);
        verify(connectionProvider).obtainConnection(eq(endpoint));
    }

    @Test
    public void endpoint_Is_Always_Non_Null() throws Exception {
        Endpoint endpoint = new Endpoint("somehost", 12345);
        ApiChannel apiChannel = new RealApiChannel(connectionProvider, endpoint);

        assertEquals(endpoint, apiChannel.endpoint());
        assertNotNull(apiChannel.endpoint());
        apiChannel.close();
        assertNotNull(apiChannel.endpoint());
    }

    @Test
    public void writer_Is_Always_Non_Null() throws Exception {
        ApiChannel apiChannel = new RealApiChannel(connectionProvider, Endpoint.DEFAULT);
        assertNotNull(apiChannel.writer());
        apiChannel.close();
        assertNotNull(apiChannel.writer());
    }

    @Test
    public void reader_Is_Always_Non_Null() throws Exception {
        ApiChannel apiChannel = new RealApiChannel(connectionProvider, Endpoint.DEFAULT);
        assertNotNull(apiChannel.reader());
        apiChannel.close();
        assertNotNull(apiChannel.reader());
    }

    @Test
    public void writer_Writes_To_Connection_Sink() throws Exception {
        ApiChannel apiChannel = new RealApiChannel(connectionProvider, Endpoint.DEFAULT);

        assertTrue(connection.writeBuffer().size() == 0);
        apiChannel.writer()
                .beginRequest()
                .writeMethodName("somename")
                .endRequest();
        assertTrue(connection.writeBuffer().size() > 0);
    }

    private ByteString mockResponse() throws IOException {
        return new ResponseBytesWriter()
                .writeValue("result", 5000)
                .bytes();
    }

    private ByteString mockDataResponse() throws IOException {
        return new ResponseBytesWriter()
                .setData(ByteString.encodeUtf8("Some data"))
                .bytes();
    }


    @Test
    public void close_Recycles_Connection_When_No_Requests_Are_Sent() throws Exception {
        ApiChannel apiChannel = spy(new RealApiChannel(connectionProvider, Endpoint.DEFAULT));
        apiChannel.close();
        verify(connectionProvider).recycleConnection(eq(connection));
        verify(connection, times(0)).close();
    }

    @Test
    public void close_Closes_Connection_When_Responses_Are_Fully_Read() throws Exception {
        ApiChannel apiChannel = spy(new RealApiChannel(connectionProvider, Endpoint.DEFAULT));

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
        verify(connectionProvider, times(1)).recycleConnection(eq(connection));
        verify(connection, times(0)).close();
    }

    @Test
    public void close_Closes_Connection_With_Pending_Responses() throws Exception {
        ApiChannel apiChannel = spy(new RealApiChannel(connectionProvider, Endpoint.DEFAULT));

        apiChannel.writer()
                .beginRequest()
                .writeMethodName("somename")
                .endRequest();

        apiChannel.close();
        verify(connection).close();
        verify(connectionProvider, times(0)).recycleConnection(eq(connection));
    }

    @Test
    public void close_Closes_Connection_With_Unfinished_Response() throws Exception {
        ApiChannel apiChannel = spy(new RealApiChannel(connectionProvider, Endpoint.DEFAULT));

        apiChannel.writer()
                .beginRequest()
                .writeMethodName("somename")
                .endRequest();

        connection.readBuffer().write(mockResponse());
        ProtocolResponseReader responseReader = apiChannel.reader();
        responseReader.beginResponse();

        apiChannel.close();
        verify(connection).close();
        verify(connectionProvider, times(0)).recycleConnection(eq(connection));
    }

    @Test
    public void close_Closes_Connection_With_Finished_Response_And_Unfinished_Data() throws Exception {
        ApiChannel apiChannel = spy(new RealApiChannel(connectionProvider, Endpoint.DEFAULT));

        apiChannel.writer()
                .beginRequest()
                .writeMethodName("somename")
                .endRequest();

        connection.readBuffer().write(mockDataResponse());
        ProtocolResponseReader responseReader = apiChannel.reader();
        responseReader.beginResponse();
        responseReader.endResponse();

        apiChannel.close();
        verify(connection).close();
        verify(connectionProvider, times(0)).recycleConnection(eq(connection));
    }


}