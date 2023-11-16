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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcloud.networking.protocol.ResponseBytesWriter;

import org.assertj.core.api.ThrowableAssert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okio.Buffer;
import okio.ByteString;

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
        realExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), r -> new Thread(r, "PCloud API Client"));
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
    public void testCallExecutesOnSpecifiedEndpoint() throws Exception {
        Endpoint endpoint = new Endpoint("test.pcloud.com", 443);
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        when(connectionProvider.obtainConnection(endpoint)).thenReturn(connection);
        MultiCall multiCall = createMultiCall(connection, executor, endpoint);

        multiCall.execute();

        assertTrue(multiCall.isExecuted());
        verify(connectionProvider, times(1)).obtainConnection(endpoint);
    }

    @Test
    public void testSuccessfulResponseRecyclesTheConnection() throws Exception {
        Connection connection = createDummyConnection(Endpoint.DEFAULT, getMockByteDataResponse(1));
        retrofitConnectionProvider(connection);

        MultiCall multiCall = createMultiCall(connection, executor);

        multiCall.execute();
        verifyConnectionObtained();
        verifyConnectionRecycled(connection);
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

        verifyConnectionObtained();
        verifyConnectionRecycled(connection);
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

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable) args[0];
                runnable.run();
                return null;
            }
        }).when(executor).execute(notNull(Runnable.class));
        MultiCallback callback = mock(MultiCallback.class);

        call.enqueue(callback);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        verify(callback).onComplete(eq(call), notNull(MultiResponse.class));
        verify(callback, times(call.requests().size())).onResponse(eq(call), captor.capture(), any(Response.class));

        List<Integer> values = captor.getAllValues();
        for (int i = 0; i < values.size(); i++) {
            assertTrue(values.contains(i));
        }

        verifyConnectionObtained();
        verifyConnectionRecycled(connection);
    }

    private void verifyConnectionRecycled(Connection connection) {
        verify(connectionProvider, times(1)).recycleConnection(connection);
        verify(connection, never()).close();
    }

    private void verifyConnectionClosed(Connection connection) {
        verify(connectionProvider, never()).recycleConnection(connection);
        verify(connection, times(1)).close();
    }

    private void verifyConnectionObtained() throws IOException {
        verify(connectionProvider, times(1)).obtainConnection();
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

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable) args[0];
                runnable.run();
                return null;
            }
        }).when(executor).execute(notNull(Runnable.class));
        MultiCallback callback = mock(MultiCallback.class);

        call.enqueue(callback);

        verify(callback).onResponse(eq(call), eq(0), notNull(Response.class));
        verify(callback, never()).onComplete(eq(call), notNull(MultiResponse.class));
        verify(callback).onFailure(eq(call), notNull(IOException.class), notNull(List.class));

        verifyConnectionObtained();
        verifyConnectionClosed(connection);
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
        try (Interactor interactor = call.start()) {
            RequestBytesWriter requestBytesWriter = new RequestBytesWriter();
            assertEquals(interactor.submitRequests(1), 1);
            assertEquals(connection.sink().buffer().readByteString(), requestBytesWriter.bytes(expectedRequest1));

            assertEquals(interactor.submitRequests(1), 1);
            assertEquals(connection.sink().buffer().readByteString(), requestBytesWriter.bytes(expectedRequest2));

            assertFalse(interactor.hasMoreRequests());
            assertEquals(interactor.submitRequests(Integer.MAX_VALUE), 0);
        }
    }

    @Test
    public void start_Returns_NonNull_Interactor() throws IOException {
        MultiCall call = createMultiCall(Request.create().methodName("something").build());
        try (Interactor interactor = call.start()) {
            assertNotNull(interactor);
        }
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
        try (Interactor interactor = call.start()) {
            expectedException.expect(IllegalStateException.class);
            interactor.nextResponse();
        }
    }

    @Test
    public void interactor_ReadsResponses() throws IOException {

        Request request1 = Request.create()
                .methodName("someApiMethod")
                .body(RequestBody.EMPTY)
                .build();

        List<ResponseBytesWriter> expectedResponses = Arrays.asList(
                new ResponseBytesWriter()
                        .beginObject()
                        .writeValue("id", 0)
                        .writeValue("result", 0)
                        .endObject(),
                new ResponseBytesWriter()
                        .beginObject()
                        .writeValue("id", 1)
                        .writeValue("result", 5000)
                        .writeValue("error", "Something went ka-boom.")
                        .endObject(),
                new ResponseBytesWriter()
                        .beginObject()
                        .writeValue("id", 2)
                        .writeValue("result", 2000)
                        .writeValue("error", "Token went ka-boom.")
                        .endObject());
        Connection connection = spy(DummyConnection.withResponses(expectedResponses));
        retrofitConnectionProvider(connection);

        MultiCall call = createMultiCall(request1, request1, request1);
        try (Interactor interactor = call.start()) {
            interactor.submitRequests(Integer.MAX_VALUE);


            while (interactor.hasNextResponse()) {
                assertContainsResponse(expectedResponses, interactor.nextResponse());
            }

            assertFalse(interactor.hasNextResponse());

            expectedException.expect(IllegalStateException.class);
            interactor.nextResponse();
        }
        verifyConnectionObtained();
        verifyConnectionRecycled(connection);
    }

    private static void assertContainsResponse(Collection<ResponseBytesWriter> responses, Response response) throws IOException {
        Buffer buffer = new okio.Buffer()
                .writeIntLe((int) response.responseBody().contentLength());
        response.responseBody().writeTo(buffer);
                ByteString responseBytes = buffer.snapshot();
        for (ResponseBytesWriter writer : responses) {
            ByteString expected = writer.bytes();
            if (expected.equals(responseBytes)) {
                return;
            }
        }

        throw new AssertionError("Response does not match any of the expected responses.");
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
                , executor, null);
    }

    private MultiCall createMultiCall(Connection connection, ExecutorService executor, Endpoint endpoint) {
        return createMultiCall(
                Collections.singletonList(RequestUtils.getUserInfoRequest(connection.endpoint()))
                , executor, endpoint);
    }

    private void retrofitConnectionProvider(Connection connection) throws IOException {
        when(connectionProvider.obtainConnection())
                .thenReturn(connection);
    }

    private Connection createDummyConnection(Endpoint endpoint, byte[] data) {
        return spy(new DummyConnection(endpoint, data));
    }

    private MultiCall createMultiCall(Request... requests) {
        return createMultiCall(Arrays.asList(requests), executor, null);
    }

    private MultiCall createMultiCall(List<Request> requests, ExecutorService executor) {
        return createMultiCall(requests, executor, null);
    }

    private MultiCall createMultiCall(List<Request> requests, ExecutorService executor, Endpoint endpoint) {
        return spy(new RealMultiCall(requests,
                executor, new ArrayList<RequestInterceptor>(), connectionProvider, endpoint));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        realExecutor.shutdown();
    }
}