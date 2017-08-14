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
import okio.Buffer;
import okio.Sink;
import okio.Source;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


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

        Source source = spy(new DummyBufferedSource(dummyConnection.source()));
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

        Sink sink = spy(new DummyBufferedSink(dummyConnection.sink()));
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

        Sink sink = spy(new DummyBufferedSink(dummyConnection.sink()));
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
