package com.pcloud.networking.client;

import com.pcloud.networking.protocol.DummyBufferedSink;
import com.pcloud.networking.protocol.DummyBufferedSource;
import okio.Buffer;
import okio.Sink;
import okio.Source;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Created by Dimitard on 10.8.2017 г..
 */
public class ForwardingConnectionTest {

    private static final byte[] MOCK_EMPTY_ARRAY_RESPONSE = new byte[] {2, 0, 0, 0, 16, -1};
    private EndpointProvider endpointProvider;

    @Before
    public void setUp() throws Exception {
        endpointProvider = mock(EndpointProvider.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionSourceRead_IOException_ReportsEndpointConnectionError() throws Exception {

        Connection dummyConnection = createDummyConnection(Endpoint.DEFAULT, MOCK_EMPTY_ARRAY_RESPONSE);

        Source source = Mockito.spy(new DummyBufferedSource(dummyConnection.source()));
        Source wrappingSource = spy(new ForwardingConnection.ReportingSource(source, endpointProvider, dummyConnection.endpoint()));
        doThrow(new IOException("Endpoint connection error")).when(source).read(any(Buffer.class), anyLong());
        try {
            wrappingSource.read(new Buffer(), 4);
        } catch (IOException e) {
            verify(endpointProvider).endpointConnectionError(Endpoint.DEFAULT, e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionSinkWrite_IOException_ReportsEndpointConnectionError() throws Exception {

        Connection dummyConnection = createDummyConnection(Endpoint.DEFAULT, MOCK_EMPTY_ARRAY_RESPONSE);

        Sink sink = Mockito.spy(new DummyBufferedSink(dummyConnection.sink()));
        Sink wrappingSource = spy(new ForwardingConnection.ReportingSink(sink, endpointProvider, dummyConnection.endpoint()));
        doThrow(new IOException("Endpoint connection error")).when(sink).write(any(Buffer.class), anyLong());
        try {
            wrappingSource.write(new Buffer(), 4);
        } catch (IOException e) {
            verify(endpointProvider).endpointConnectionError(Endpoint.DEFAULT, e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionSinkFlush_IOException_ReportsEndpointConnectionError() throws Exception {

        Connection dummyConnection = createDummyConnection(Endpoint.DEFAULT, MOCK_EMPTY_ARRAY_RESPONSE);

        Sink sink = Mockito.spy(new DummyBufferedSink(dummyConnection.sink()));
        Sink wrappingSource = spy(new ForwardingConnection.ReportingSink(sink, endpointProvider, dummyConnection.endpoint()));
        doThrow(new IOException("Endpoint connection error")).when(sink).flush();
        try {
            wrappingSource.flush();
        } catch (IOException e) {
            verify(endpointProvider).endpointConnectionError(Endpoint.DEFAULT, e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionClose_ClosesTheWrappedConnection() throws Exception {

        Connection dummyConnection = createDummyConnection(Endpoint.DEFAULT, MOCK_EMPTY_ARRAY_RESPONSE);

        ForwardingConnection forwardingConnection = spy(new ForwardingConnection(dummyConnection, endpointProvider));
        forwardingConnection.close();
        verify(dummyConnection).close();
    }

    private Connection createDummyConnection(Endpoint endpoint, byte[] data) {
        return spy(new DummyConnection(endpoint, data));
    }
}
