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

import org.assertj.core.api.ThrowableAssert;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RealMultiCallTest {

    private static final int MOCK_TIMEOUT_TIME = 500;
    private static final int EMPTY_ARRAY_RESPONSE_LENGTH = 10;

    private ExecutorService executor;
    private static ExecutorService realExecutor;
    private ConnectionProvider connectionProvider;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        retrofitConnectionProvider(connection);
        MultiCall multiCall = createMultiCall(connection, executor);

        multiCall.execute();

        assertTrue(multiCall.isExecuted());
    }

    @Test
    public void testSuccessfulResponseRecyclesTheConnection() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        retrofitConnectionProvider(connection);

        MultiCall multiCall = createMultiCall(connection, executor);

        multiCall.execute();

        verify(connectionProvider).recycleConnection(connection);
    }

    @Test
    public void testExecutingTwiceThrowsIllegalStateException() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        retrofitConnectionProvider(connection);

        final MultiCall multiCall = createMultiCall(connection, executor);

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
        retrofitConnectionProvider(connection);

        final MultiCall multiCall = createMultiCall(connection, executor);

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

        retrofitConnectionProvider(connection);

        final MultiCall multiCall = createMultiCall(connection, executor);

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
        retrofitConnectionProvider(connection);

        final MultiCall multiCall = createMultiCall(connection, realExecutor);

        multiCall.enqueueAndWait();

        verify(connectionProvider).obtainConnection(connection.endpoint());
        verify(connectionProvider).recycleConnection(connection);
        verify(connection, never()).close();
    }

    @Test
    public void testEnqueueWithTimeoutBlocksUntilTimeout() throws Exception {
        final Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        retrofitConnectionProvider(connection);


        final MultiCall multiCall = createMultiCall(connection, realExecutor);

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
        retrofitConnectionProvider(connection);

        final MultiCall call = createMultiCall(requestList, executor);

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

        verify(callback).onComplete(eq(call), notNull(MultiResponse.class));
        verify(connectionProvider).recycleConnection(connection);
        verify(callback, times(call.requests().size())).onResponse(eq(call), captor.capture(), any(Response.class));

        List<Integer> values = captor.getAllValues();
        for (int i = 0; i < values.size(); i++) {
            assertTrue(values.contains(i));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSuccessfulEnqueueReportsFailuresToTheCallback() throws Exception {
        List<Request> requestList = getMockRequestList(Endpoint.DEFAULT, 3);
        byte[] responseData = getMockByteDataResponse(requestList.size());
        responseData[EMPTY_ARRAY_RESPONSE_LENGTH + 1] = -1;

        final Connection connection = createDummyConnection(Endpoint.DEFAULT, responseData);
        retrofitConnectionProvider(connection);

        final MultiCall call = createMultiCall(requestList, executor);

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

        verify(callback).onResponse(eq(call), eq(0), notNull(Response.class));
        verify(callback, never()).onComplete(eq(call), notNull(MultiResponse.class));
        verify(callback).onFailure(eq(call), notNull(IOException.class), notNull(List.class));
        verify(connectionProvider, never()).recycleConnection(connection);
        verify(connection).close();
    }

@Test
    public void test_Interactor_WritesRequests() throws IOException {

        Request request1 = Request.create()
                .methodName("someApiMethod")
                .body(TestRequestBody.create()
                        .setValue("arg1", "value1")
                        .setValue("arg2", "value2")
                        .setValue("arg3", "value3")
                        .setValue("arg4", "value4")
                        .build())
                .build();

        Request request2 = request1.newRequest()
                .body(TestRequestBody.create()
                        .setValue("arg5", "value5")
                        .setValue("arg6", "value6")
                        .setValue("arg7", "value7")
                        .setValue("arg8", "value8")
                        .build())
                .build();

    Request expectedRequest1 = Request.create()
            .methodName("someApiMethod")
            .body(TestRequestBody.create()
                    .setValue("id", 0)
                    .setValue("arg1", "value1")
                    .setValue("arg2", "value2")
                    .setValue("arg3", "value3")
                    .setValue("arg4", "value4")
                    .build())
            .build();

    Request expectedRequest2 = request1.newRequest()
            .body(TestRequestBody.create()
                    .setValue("id", 1)
                    .setValue("arg5", "value5")
                    .setValue("arg6", "value6")
                    .setValue("arg7", "value7")
                    .setValue("arg8", "value8")
                    .build())
            .build();

        Connection connection = spy(new DummyConnection());
        retrofitConnectionProvider(connection);
        MultiCall call = createMultiCall(request1, request2);
        Interactor interactor = call.start();

        RequestWriter requestWriter = new RequestWriter();
        assertEquals(interactor.submitRequests(1), 1);
        assertEquals(connection.sink().buffer().readByteString(), requestWriter.bytes(expectedRequest1));

        assertEquals(interactor.submitRequests(1), 1);
        assertEquals(connection.sink().buffer().readByteString(), requestWriter.bytes(expectedRequest2));

        assertFalse(interactor.hasMoreRequests());
        assertEquals(interactor.submitRequests(Integer.MAX_VALUE), 0);
    }

    @Test
    public void start_Returns_NonNull_Interactor() throws IOException {
        MultiCall call = createMultiCall(Request.create().methodName("something").build());
        assertNotNull(call.start());
    }

    @Test
    public void interactor_Throws_If_Reading_BeforeWriting() throws IOException {
        Request request1 = Request.create()
                .methodName("someApiMethod")
                .body(TestRequestBody.create()
                        .setValue("arg1", "value1")
                        .setValue("arg2", "value2")
                        .setValue("arg3", "value3")
                        .setValue("arg4", "value4")
                        .build())
                .build();
        Connection connection = spy(DummyConnection.withResponses());
        retrofitConnectionProvider(connection);
        MultiCall call = createMultiCall(request1);
        Interactor interactor = call.start();

        expectedException.expect(IllegalStateException.class);
        interactor.nextResponse();
    }

    @Test
    public void interactor_ReadsResponses() throws IOException {

        Request request1 = Request.create()
                .methodName("someApiMethod")
                .body(RequestBody.EMPTY)
                .build();

        List<ResponseBytes> expectedResponses = Arrays.asList(
                new ResponseBytes()
                        .writeValue("id", 0)
                        .writeValue("result", 0),
                new ResponseBytes()
                        .writeValue("id", 1)
                        .writeValue("result", 5000)
                        .writeValue("error", "Something went ka-boom."),
                new ResponseBytes()
                        .writeValue("id", 2)
                        .writeValue("result", 2000)
                        .writeValue("error", "Token went ka-boom."));
        Connection connection = spy(DummyConnection.withResponses(expectedResponses));
        retrofitConnectionProvider(connection);

        MultiCall call = createMultiCall(request1, request1, request1);
        Interactor interactor = call.start();
        interactor.submitRequests(Integer.MAX_VALUE);

        while (interactor.hasNextResponse()){
            assertContainsResponse(expectedResponses, interactor.nextResponse());
        }

        assertFalse(interactor.hasNextResponse());
        verify(connectionProvider, times(1)).recycleConnection(eq(connection));

        expectedException.expect(IllegalStateException.class);
        interactor.nextResponse();
    }

    private static void assertContainsResponse(Collection<ResponseBytes> responses, Response response) throws IOException {
        Map<String,?> responseValues = response.responseBody().toValues();

        Collection<Map<String,?>> expectedValues = new ArrayList<>(responses.size());
        for (ResponseBytes bytes : responses){
            expectedValues.add(bytes.toValues());
        }

        assertThat(expectedValues, Matchers.hasItem(responseValues));
    }

    private byte[] getMockByteDataResponse(int numberOfRequests) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(numberOfRequests * EMPTY_ARRAY_RESPONSE_LENGTH);
        for (int i = 0; i < numberOfRequests; i++) {
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

    private MultiCall createMultiCall(Connection connection, ExecutorService executor) {
        return createMultiCall(
                Collections.singletonList(RequestUtils.getUserInfoRequest(connection.endpoint()))
                , executor);
    }

    private void retrofitConnectionProvider(Connection connection) throws IOException {
        when(connectionProvider.obtainConnection(connection.endpoint()))
                .thenReturn(connection);
    }

    private Connection createDummyConnection(Endpoint endpoint, byte[] data) {
        return spy(new DummyConnection(endpoint, data));
    }

    private MultiCall createMultiCall(Request... requests) {
        return createMultiCall(Arrays.asList(requests), executor);
    }

    private MultiCall createMultiCall(ExecutorService executor, Request... requests) {
        return createMultiCall(Arrays.asList(requests), executor);
    }

    private MultiCall createMultiCall(List<Request> requests, ExecutorService executor) {
        return spy(new RealMultiCall(requests,
                executor, new ArrayList<RequestInterceptor>(), connectionProvider));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        realExecutor.shutdown();
    }
}