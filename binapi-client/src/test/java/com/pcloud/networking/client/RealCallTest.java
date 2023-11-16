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

import com.pcloud.networking.protocol.BytesReader;
import com.pcloud.networking.protocol.ProtocolReader;
import okio.BufferedSink;
import okio.Okio;
import org.assertj.core.api.ThrowableAssert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static okio.Okio.blackhole;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RealCallTest {

    private static final String MOCK_HOST = "mockbinapi@pcloud.com";
    private static final int MOCK_PORT = 123;
    private static final int MOCK_TIMEOUT_TIME = 250;
    private static final byte[] MOCK_EMPTY_ARRAY_RESPONSE = new byte[]{2, 0, 0, 0, 16, -1};


    private ExecutorService executor;
    private static ExecutorService realExecutor;
    private ConnectionProvider connectionProvider;


    @BeforeClass
    public static void initialSetup() throws Exception {
        realExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), r -> new Thread(r, "PCloud API Client"));
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

        RealCall call = createCall(request, executor);

        call.execute();
        assertTrue(call.isExecuted());
    }

    @Test
    public void testCallExecutesOnSpecifiedRequestEndpoint() throws Exception {
        Endpoint endpoint = new Endpoint("test.pcloud.com", 443);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        mockConnection(createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE));

        RealCall call = createCall(request, executor);

        call.execute();
        assertTrue(call.isExecuted());
        verifyConnectionObtained(endpoint);
    }

    private void verifyConnectionObtained(Endpoint endpoint) {
        try {
            verify(connectionProvider, times(1)).obtainConnection(endpoint);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testExecutingTwiceThrowsIllegalStateException() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        mockConnection(createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE));

        final RealCall call = createCall(request, executor);

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

        final RealCall call = createCall(request, executor);
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

        final RealCall call = createCall(request, executor);

        try {
            call.execute();
            fail();
        } catch (IOException e) {
        }
        verifyConnectionObtained(request.endpoint());
        verifyConnectionClosed(connection);
    }

    @Test
    public void testEnqueueAndWaitBlocksUntilResponse() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Connection connection = createDummyConnection(endpoint, MOCK_EMPTY_ARRAY_RESPONSE);
        mockConnection(connection);
        Request request = RequestUtils.getUserInfoRequest(endpoint);

        final RealCall call = createCall(request, realExecutor);

        try (Response response = call.enqueueAndWait()) {
            readResponse((BytesReader) response.responseBody().reader());
        }
        verifyConnectionObtained(endpoint);
        verifyConnectionRecycled(connection);
    }

    private void verifyConnectionRecycled(Connection connection) {
        verify(connectionProvider, times(1)).recycleConnection(connection);
        verify(connection, never()).close();
    }

    @Test
    public void testEnqueueWithTimeoutBlocksUntilTimeout() throws Exception {
        final Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        final Connection connection = createDummyConnection(Endpoint.DEFAULT, MOCK_EMPTY_ARRAY_RESPONSE);
        mockConnection(connection);

        final RealCall call = createCall(request, realExecutor);

        doAnswer(new Answer<BufferedSink>() {
            @Override
            public BufferedSink answer(InvocationOnMock invocation) throws Throwable {
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
        verifyConnectionObtained(request.endpoint());
        verifyConnectionClosed(connection);
    }

    @Test
    public void testSuccessfulEnqueueReportsResultToTheCallback() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        final Connection connection = createDummyConnection(endpoint, MOCK_EMPTY_ARRAY_RESPONSE);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        mockConnection(connection);

        final RealCall call = createCall(request, executor);

        final CountDownLatch latch = new CountDownLatch(1);
        Callback callback = spy(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    response.responseBody().writeTo(Okio.buffer(blackhole()));
                } finally {
                    response.close();
                }
                latch.countDown();
            }
        });
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable) args[0];
                runnable.run();
                return null;
            }
        }).when(executor).execute(notNull(Runnable.class));

        call.enqueue(callback);
        latch.await();

        verify(callback, times(1)).onResponse(eq(call), notNull(Response.class));
        verify(callback, never()).onFailure(eq(call), notNull(IOException.class));
        verifyConnectionObtained(request.endpoint());
        verifyConnectionRecycled(connection);
    }

    @Test
    public void testExceptionDuringEnqueuingReportsTheFailureToTheCallback() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        final Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);
        when(connection.source()).thenThrow(IOException.class);

        mockConnection(connection);

        final RealCall call = createCall(request, executor);

        final CountDownLatch latch = new CountDownLatch(1);
        Callback callback = spy(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.responseBody().writeTo(Okio.buffer(blackhole()));
                latch.countDown();
            }
        });
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable) args[0];
                runnable.run();
                return null;
            }
        }).when(executor).execute(notNull(Runnable.class));

        call.enqueue(callback);
        latch.await();

        verify(callback, times(1)).onFailure(eq(call), notNull(IOException.class));
        verify(callback, never()).onResponse(eq(call), notNull(Response.class));
        verifyConnectionObtained(request.endpoint());
        verifyConnectionClosed(connection);
    }

    @Test
    public void testClosingTheResponseBodyBeforeFullyReadingItClosesTheConnection() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);
        final RealCall call = createCall(request, executor);

        try (Response response = call.execute()) {
            ProtocolReader reader = response.responseBody().reader();
            reader.beginObject();
        }

        verifyConnectionObtained(request.endpoint());
        verifyConnectionClosed(connection);
    }

    @Test
    public void testClosingTheResponseBodyAfterFullyReadingItRecyclesTheConnection() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = createCall(request, executor);

        try (Response response = call.execute()) {
            readResponse((BytesReader) response.responseBody().reader());
        }

        verifyConnectionObtained(endpoint);
        verifyConnectionRecycled(connection);
    }

    @Test
    public void testConnectionProviderSearchesForConnectionOnTheRequestEndpoint() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = createCall(request, executor);
        call.execute();

        verifyConnectionObtained(endpoint);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionCloseAfterExceptionDuringRequestWriting() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = createCall(request, executor);
        when(connection.sink()).thenThrow(IOException.class);

        try {
            call.execute();
            fail();
        } catch (IOException e) {
            verifyConnectionObtained(endpoint);
            verifyConnectionClosed(connection);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionCloseAfterExceptionDuringResponseRead() throws Exception {
        Endpoint endpoint = new Endpoint(MOCK_HOST, MOCK_PORT);
        Request request = RequestUtils.getUserInfoRequest(endpoint);
        Connection connection = createDummyConnection(request.endpoint(), MOCK_EMPTY_ARRAY_RESPONSE);

        mockConnection(connection);

        final RealCall call = createCall(request, executor);
        when(connection.source()).thenThrow(IOException.class);

        try {
            call.execute();
            fail();
        } catch (IOException e) {
            verifyConnectionObtained(endpoint);
            verifyConnectionClosed(connection);
        }
    }

    private void verifyConnectionClosed(Connection connection) {
        verify(connection, times(1)).close();
        verify(connectionProvider, never()).recycleConnection(connection);
    }

    private void readResponse(BytesReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
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

    private RealCall createCall(Request request, ExecutorService executor) {
        return new RealCall(request,
                executor, new ArrayList<RequestInterceptor>(), connectionProvider);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        realExecutor.shutdown();
    }

}