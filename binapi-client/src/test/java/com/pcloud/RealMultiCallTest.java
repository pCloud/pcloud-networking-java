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

import okio.BufferedSource;
import org.assertj.core.api.ThrowableAssert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * Created by Dimitard on 19.4.2017 г..
 */
public class RealMultiCallTest {

    private static final int MOCK_TIMEOUT_TIME = 500;
    private static final int EMPTY_ARRAY_RESPONSE_LENGTH = 10;

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
    public void testExecuteMarksTheMultiCallAsExecuted() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        mockConnection(connection);

        RealMultiCall multiCall = getMockRealMultiCall(connection, executor);

        multiCall.execute();

        assertTrue(multiCall.isExecuted());
    }

    @Test
    public void testSuccessfulResponseRecyclesTheConnection() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        mockConnection(connection);

        RealMultiCall multiCall = getMockRealMultiCall(connection, executor);

        multiCall.execute();

        verify(connectionProvider).recycleConnection(connection);
    }

    @Test
    public void testExecutingTwiceThrowsIllegalStateException() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        mockConnection(connection);

        final RealMultiCall multiCall = getMockRealMultiCall(connection, executor);

        multiCall.execute();

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                multiCall.execute();
            }
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testExecutingAfterCancelThrowsIOException() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        mockConnection(connection);

        final RealMultiCall multiCall = getMockRealMultiCall(connection, executor);

        multiCall.cancel();

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                multiCall.execute();
            }
        }).isInstanceOf(IOException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectionCloseAfterException() throws Exception {
        Request request = RequestUtils.getUserInfoRequest(Endpoint.DEFAULT);
        Connection connection = createDummyConnection(request.endpoint(), getMockByteDataResponse(1));
        when(connection.source()).thenThrow(IOException.class);

        mockConnection(connection);

        final RealMultiCall multiCall = getMockRealMultiCall(connection, executor);

        try {
            multiCall.execute();
        } catch (IOException e) {
            verify(connection).close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEnqueueAndWaitBlocksUntilResponse() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        mockConnection(connection);

        final RealMultiCall multiCall = getMockRealMultiCall(connection, realExecutor);

        multiCall.enqueueAndWait();

        verify(connectionProvider).obtainConnection(connection.endpoint());
        verify(connectionProvider).recycleConnection(connection);
        verify(connection, never()).close();
    }

    @Test
    public void testEnqueueWithTimeoutBlocksUntilTimeout() throws Exception {
        final Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        mockConnection(connection);


        final RealMultiCall multiCall = getMockRealMultiCall(connection, realExecutor);

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
                multiCall.enqueueAndWait(MOCK_TIMEOUT_TIME, TimeUnit.MILLISECONDS);
            }
        }).isInstanceOf(TimeoutException.class);
    }

    @Test
    public void testSuccessfulEnqueueReportsResultsToTheCallback() throws Exception {
        List<Request> requestList = getMockRequestList(Endpoint.DEFAULT, 3);
        final Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(requestList.size()));
        mockConnection(connection);

        final RealMultiCall call = getMockRealMultiCall(requestList, executor);

        when(executor.submit(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = ((Runnable) args[0]);
                runnable.run();
                return new FutureTask<Void>(runnable, null);
            }
        });
        MultiCallback callback = mock(MultiCallback.class);

        call.enqueue(callback);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        verify(callback).onComplete(eq(call), any(MultiResponse.class));
        verify(connectionProvider).recycleConnection(connection);
        verify(callback, times(call.requests().size())).onResponse(eq(call), captor.capture(), any(Response.class));

        List<Integer> values = captor.getAllValues();
        for(int i = 0; i < values.size(); i++) {
            assertTrue(values.contains(i));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSuccessfulEnqueueReportsFailuresToTheCallback() throws Exception {
        List<Request> requestList = getMockRequestList(Endpoint.DEFAULT, 3);
        final Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(requestList.size()));
        BufferedSource connectionSource = connection.source();
        when(connection.source()).thenReturn(connectionSource).thenThrow(IOException.class);
        mockConnection(connection);

        final RealMultiCall call = getMockRealMultiCall(requestList, executor);

        when(executor.submit(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = ((Runnable) args[0]);
                runnable.run();
                return new FutureTask<Void>(runnable, null);
            }
        });
        MultiCallback callback = mock(MultiCallback.class);

        call.enqueue(callback);

        verify(callback).onResponse(eq(call), eq(0), any(Response.class));
        verify(callback, never()).onComplete(eq(call), any(MultiResponse.class));
        verify(callback).onFailure(eq(call), any(IOException.class), any(List.class));
        verify(connectionProvider, never()).recycleConnection(connection);
        verify(connection).close();
    }



    private byte[] getMockByteDataResponse(int numberOfRequests) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(numberOfRequests*EMPTY_ARRAY_RESPONSE_LENGTH);
        for(int i = 0; i < numberOfRequests; i++) {
            byteBuffer.put(new byte[]{6, 0, 0, 0, 16, 102, 105, 100, (byte) (200 + i), -1});
        }
        return byteBuffer.array();
    }

    private List<Request> getMockRequestList(Endpoint endpoint, int size) {
        List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            requestList.add(RequestUtils.getUserInfoRequest(endpoint));
        }
        return requestList;
    }

    private RealMultiCall getMockRealMultiCall(Connection connection, ExecutorService executor) {
        return getMockRealMultiCall(
                Collections.singletonList(RequestUtils.getUserInfoRequest(connection.endpoint()))
                , executor);
    }

    private void mockConnection(Connection connection) throws IOException {
        when(connectionProvider.obtainConnection(connection.endpoint()))
                .thenReturn(connection);
    }

    private Connection createDummyConnection(Endpoint endpoint, byte[] data) {
        return spy(new DummyConnection(endpoint, data));
    }

    private RealMultiCall getMockRealMultiCall(List<Request> requests, ExecutorService executor) {
        return spy(new RealMultiCall(requests,
                executor, new ArrayList<RequestInterceptor>(), connectionProvider));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        realExecutor.shutdown();
    }
}