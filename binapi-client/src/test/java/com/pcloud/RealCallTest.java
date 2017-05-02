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

package com.pcloud;

import com.pcloud.protocol.streaming.BytesReader;
import org.assertj.core.api.ThrowableAssert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RealCallTest {

    private static final String MOCK_HOST = "mockbinapi@pcloud.com";
    private static final int    MOCK_PORT = 123;
    private static final int    MOCK_TIMEOUT_TIME = 250;
    private static final byte[] MOCK_EMPTY_ARRAY_RESPONSE = new byte[] {2, 0, 0, 0, 16, -1};


    private ExecutorService executor;
    private static ExecutorService realExecutor;
    private ConnectionProvider connectionProvider;


    @BeforeClass
    public static void initialSetup() throws Exception {
        realExecutor = spy(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "PCloud API Client");
            }
        }));
    }

    @Before
    public void setUp() throws Exception {
        executor = mock(ThreadPoolExecutor.class);
        connectionProvider = mock(ConnectionProvider.class);
    }

    @Test
    public void testExecuteMarksTheCallAsExecuted() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        mockConnection(createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE));

        RealCall call = getMockRealCall(request, executor);

        call.execute();
        assertTrue(call.isExecuted());
    }

    @Test
    public void testExecutingTwiceThrowsIllegalStateException() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        mockConnection(createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE));

        final RealCall call = getMockRealCall(request, executor);

        call.execute();

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                call.execute();
            }
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testExecutingAfterCancelThrowsIOException() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        mockConnection(createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE));

        final RealCall call = getMockRealCall(request, executor);
        call.cancel();

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                call.execute();
            }
        }).isInstanceOf(IOException.class);
    }

    @Test
    public void testConnectionCloseAfterException() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);
        doThrow(IOException.class).when(connection).source();

        mockConnection(connection);

        final RealCall call = getMockRealCall(request, executor);

        try {
            call.execute();
        } catch (IOException e) {
            verify(connection).close();
        }
    }

    @Test
    public void testEnqueueAndWaitBlocksUntilResponse() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Connection connection = createDummyConnection(endpoint, MOCK_EMPTY_ARRAY_RESPONSE);
        mockConnection(connection);
        Request request = RequestUtils.getUserInfoRequest(endpoint);

        final RealCall call = getMockRealCall(request, realExecutor);

        Response response = call.enqueueAndWait();
        verify(connectionProvider).obtainConnection(endpoint);
        readResponse((BytesReader)response.responseBody().reader());
        response.responseBody().close();

        verify(connectionProvider).recycleConnection(connection);
    }

    @Test
    public void testEnqueueWithTimeoutBlocksUntilTimeout() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        final Connection connection = createDummyConnection(Endpoint.DEFAULT, MOCK_EMPTY_ARRAY_RESPONSE);
        mockConnection(connection);

        final RealCall call = getMockRealCall(request, realExecutor);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(MOCK_TIMEOUT_TIME);
                return connection.sink();
            }
        }).when(connection).sink();


        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                call.enqueueAndWait(MOCK_TIMEOUT_TIME, TimeUnit.MILLISECONDS);
            }
        }).isInstanceOf(TimeoutException.class);
    }

    @Test
    public void testSuccessfulEnqueueReportsResultToTheCallback() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        final Connection connection = createDummyConnection(endpoint, MOCK_EMPTY_ARRAY_RESPONSE);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        mockConnection(connection);

        final RealCall call = getMockRealCall(request, executor);

        Callback callback = mock(Callback.class);
        when(executor.submit(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable) args[0];
                runnable.run();
                return new FutureTask<Void>(runnable, null);
            }
        });

        call.enqueue(callback);

        verify(callback).onResponse(eq(call), notNull(Response.class));
        verify(callback, never()).onFailure(eq(call), notNull(IOException.class));
    }

    @Test
    public void testExceptionDuringEnqueuingReportsTheFailureToTheCallback() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        final Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);
        doThrow(IOException.class).when(connection).source();

        mockConnection(connection);

        final RealCall call = getMockRealCall(request, executor);

        Callback callback = mock(Callback.class);
        when(executor.submit(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable) args[0];
                runnable.run();
                return new FutureTask<Void>(runnable, null);
            }
        });

        call.enqueue(callback);

        verify(callback).onFailure(eq(call), notNull(IOException.class));
        verify(callback, never()).onResponse(eq(call), notNull(Response.class));
    }

    @Test
    public void testClosingTheResponseBodyBeforeFullyReadingItClosesTheConnection() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);
        final RealCall call = getMockRealCall(request, executor);

        Response response = call.execute();
        BytesReader reader = (BytesReader) response.responseBody().reader();
        reader.beginObject();
        response.responseBody().close();

        verify(connectionProvider, never()).recycleConnection(connection);
        verify(connection).close();
    }

    @Test
    public void testClosingTheResponseBodyAfterFullyReadingItRecyclesTheConnection() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = getMockRealCall(request, executor);

        Response response = call.execute();

        readResponse((BytesReader)response.responseBody().reader());
        response.responseBody().close();

        verify(connectionProvider).recycleConnection(connection);
        verify(connectionProvider).obtainConnection(endpoint);
    }

    @Test
    public void testConnectionProviderSearchesForConnectionOnTheRequestEndpoint() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = getMockRealCall(request, executor);
        call.execute();

        verify(connectionProvider).obtainConnection(endpoint);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionCloseAfterExceptionDuringRequestWriting() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = getMockRealCall(request, executor);
        when(connection.sink()).thenThrow(IOException.class);

        try {
            call.execute();
        } catch (IOException e) {
            verify(connectionProvider).obtainConnection(endpoint);
            verify(connection).close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionCloseAfterExceptionDuringResponseRead() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = getMockRealCall(request, executor);
        when(connection.source()).thenThrow(IOException.class);

        try {
            call.execute();
        } catch (IOException e) {
            verify(connectionProvider).obtainConnection(endpoint);
            verify(connection).close();
        }
    }

    private void readResponse(BytesReader reader) throws IOException {
        reader.beginObject();
        while(reader.hasNext()) {
            reader.skipValue();
        }
        reader.endObject();
    }

    private void mockConnection(Connection connection) throws IOException {
        when(connectionProvider.obtainConnection(connection.endpoint()))
                .thenReturn(connection);
    }

    private Connection createDummyConnection(Endpoint endpoint, byte[] data) {
       return spy(new DummyConnection(endpoint, data));
    }

    private RealCall getMockRealCall(Request request, ExecutorService executor) {
        return spy(new RealCall(request,
                executor, new ArrayList<RequestInterceptor>(), connectionProvider));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        realExecutor.shutdown();
    }

}