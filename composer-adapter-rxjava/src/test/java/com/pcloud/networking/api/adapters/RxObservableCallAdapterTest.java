package com.pcloud.networking.api.adapters;

import com.pcloud.networking.api.ApiResponse;
import com.pcloud.networking.api.Call;
import com.pcloud.networking.api.Interactor;
import com.pcloud.networking.api.MultiCall;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RxObservableCallAdapterTest {

    private Call<ApiResponse> mockCall;
    private MultiCall<Object, ApiResponse> mockMultiCall;
    private Interactor<ApiResponse> mockInteractor;
    private RxObservableCallAdapter<ApiResponse> callAdapter;
    private TestSubscriber<ApiResponse> subscriber;
    
    @Before
    public void setUp() throws Exception {
        callAdapter = new RxObservableCallAdapter<>(ApiResponse.class);
        mockCall = mock(Call.class);
        mockMultiCall = mock(MultiCall.class);
        mockInteractor = mock(Interactor.class);
        when(mockCall.clone()).thenReturn(mockCall);
        when(mockMultiCall.clone()).thenReturn(mockMultiCall);
        when(mockMultiCall.start()).thenReturn(mockInteractor);
        subscriber = new TestSubscriber<>();
    }

    @Test
    public void adapt_Call_Returns_NonNull_Observable() throws Exception {
        assertNotNull(callAdapter.adapt(mockCall));
    }

    @Test
    public void adapt_MultiCall_Returns_NonNull_Observable() throws Exception {
        assertNotNull(callAdapter.adapt(mockMultiCall));
    }

    @Test
    public void adapt_Call_Clones_The_Provided_Call() throws Exception {
        callAdapter.adapt(mockCall).subscribe(subscriber);
        verify(mockCall).clone();
    }

    @Test
    public void adapt_MultiCall_Clones_The_Provided_MultiCall() throws Exception {
        callAdapter.adapt(mockMultiCall).subscribe(subscriber);
        verify(mockMultiCall).clone();
    }

    @Test
    public void call_Observable_Calls_Call_execute() throws Exception {
        callAdapter.adapt(mockCall).subscribe(subscriber);
        verify(mockCall).execute();
    }

    @Test
    public void call_Observable_Emits_Result_To_Subscriber() throws Exception {
        ApiResponse response = mock(ApiResponse.class);
        when(mockCall.execute()).thenReturn(response);

        callAdapter.adapt(mockCall).subscribe(subscriber);

        subscriber.assertValues(response);
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void call_Observable_Emits_Errors_To_Subscriber() throws Exception {
        Throwable error = new IOException();
        when(mockCall.execute()).thenThrow(error);

        callAdapter.adapt(mockCall).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertError(error);
    }

    @Test
    public void call_Observable_Calls_Call_Cancel_on_Unsubscribe() throws Exception {
        ApiResponse response = mock(ApiResponse.class);
        when(mockCall.execute()).thenReturn(response);
        subscriber = new TestSubscriber<>(0);

        callAdapter.adapt(mockCall).subscribe(subscriber);

        subscriber.unsubscribe();
        verify(mockCall).cancel();
    }

    @Test
    public void multiCall_Observable_Calls_MultiCall_start() throws Exception {
        callAdapter.adapt(mockMultiCall).subscribe(subscriber);
        verify(mockMultiCall).start();
    }

    @Test
    public void multiCall_Observable_Respects_Reactive_Pull() throws Exception {
        final int pullSize = 5;
        when(mockInteractor.hasMoreRequests()).thenReturn(true);
        when(mockInteractor.hasNextResponse()).thenReturn(true);
        when(mockInteractor.submitRequests(eq(pullSize))).thenReturn(pullSize);

        subscriber.requestMore(pullSize);
        callAdapter.adapt(mockMultiCall).subscribe(subscriber);

        verify(mockInteractor).submitRequests(eq(pullSize));
    }

    @Test
    public void multiCall_Observable_Emits_Values_To_Subscriber() throws Exception {
        ApiResponse[] responses = {mock(ApiResponse.class), mock(ApiResponse.class),mock(ApiResponse.class)};
        when(mockInteractor.hasMoreRequests()).thenReturn(true, false);
        when(mockInteractor.hasNextResponse()).thenReturn(true, false);
        when(mockInteractor.submitRequests(anyInt())).thenReturn(responses.length);
        when(mockInteractor.nextResponse()).thenReturn(responses[0], responses[1], responses[2]);

        callAdapter.adapt(mockMultiCall).subscribe(subscriber);

        verify(mockInteractor, times(responses.length)).nextResponse();
        subscriber.assertNoErrors();
        subscriber.assertValues(responses);
        subscriber.assertCompleted();
        verify(mockInteractor).close();
    }

    @Test
    public void multiCall_Observable_Emits_WriteRequest_Errors_To_Subscriber() throws Exception {
        when(mockInteractor.hasMoreRequests()).thenReturn(true);
        when(mockInteractor.hasNextResponse()).thenReturn(true);

        Throwable error = new IOException();
        when(mockInteractor.submitRequests(anyInt())).thenThrow(error);

        callAdapter.adapt(mockMultiCall).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertError(error);
        verify(mockInteractor).close();
    }

    @Test
    public void multiCall_Observable_Emits_ReadResponse_Errors_To_Subscriber() throws Exception {
        when(mockInteractor.hasMoreRequests()).thenReturn(true);
        when(mockInteractor.hasNextResponse()).thenReturn(true);

        Throwable error = new IOException();
        when(mockInteractor.submitRequests(anyInt())).thenReturn(Integer.MAX_VALUE);
        when(mockInteractor.nextResponse()).thenThrow(error);

        callAdapter.adapt(mockMultiCall).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertError(error);
        verify(mockInteractor).close();
    }


    @Test
    public void multiCall_Observable_Closes_Interactor_On_Unsubscribe() throws Exception {
        subscriber.requestMore(0);

        callAdapter.adapt(mockMultiCall).subscribe(subscriber);

        subscriber.unsubscribe();
        verify(mockInteractor).close();
    }
}
