/*
 * Copyright (c) 2017 pCloud AG
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

package pcloud.networking;

import com.pcloud.RxCallAdapter;
import com.pcloud.networking.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;

public class UserInfoTest extends ApiIntegrationTest {

    private static UserApi userApi;

    @BeforeClass
    public static void setUserApi() throws Exception {
        ApiComposer apiComposer = ApiIntegrationTest.apiComposer.newBuilder()
                .addAdapterFactory(RxCallAdapter.FACTORY)
                .create();
        userApi = apiComposer.compose(UserApi.class);
    }

    @Test
    public void getUserInfo_ShouldNotFail() throws Exception {

        Observable<UserInfoResponse> observable = userApi.getUserInfo(username, password, true);
        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        assertSubscriptionCompletion(testSubscriber, 1);
    }

    @Test
    public void getUserInfo_ShouldFail_WithNullParameters() throws Exception {

        Observable<UserInfoResponse> observable = userApi.getUserInfo(null, null, false);
        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        assertSubscriptionError(testSubscriber, IllegalArgumentException.class);
    }

    @Test
    public void getUserInfo2_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        Observable<UserInfoResponse> observable = userApi.getUserInfo2(request);
        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        assertSubscriptionCompletion(testSubscriber, 1);
    }

    @Test
    public void getUserInfo2_ShouldFail_WithNullParameters() throws Exception {

        Observable<UserInfoResponse> observable = userApi.getUserInfo2(null);
        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        assertSubscriptionError(testSubscriber, IllegalArgumentException.class);
    }

    @Test
    public void getUserInfo3_ShouldNotFail() throws Exception {
        UserInfoRequest request = new UserInfoRequest(username, password, true);
        List<UserInfoRequest> list = new ArrayList<>();
        list.add(request);
        list.add(request);
        ExecutorService executor = apiClient.callExecutor();
        mockExecutorSubmit(executor);

        Observable<UserInfoResponse> observable = userApi.getUserInfo3(list);
        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        assertSubscriptionCompletion(testSubscriber, 2);
    }

    @Test
    public void getUserInfo3_ShouldFail_WithNullRequestArguments() throws Exception {
        UserInfoRequest request = new UserInfoRequest(null, null, true);
        List<UserInfoRequest> list = new ArrayList<>();
        list.add(request);
        list.add(request);
        ExecutorService executor = apiClient.callExecutor();
        mockExecutorSubmit(executor);

        Observable<UserInfoResponse> observable = userApi.getUserInfo3(list);
        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        assertSubscriptionError(testSubscriber, IllegalArgumentException.class);
    }

    @Test
    public void getUserInfo4_ShouldNotFail() throws Exception {
        UserInfoRequest request = new UserInfoRequest(username, password, true);
        List<UserInfoRequest> list = new ArrayList<>();
        list.add(request);
        list.add(request);
        ExecutorService executor = apiClient.callExecutor();
        mockExecutorSubmit(executor);

        Observable<UserInfoResponse> observable = userApi.getUserInfo4(list.toArray(new UserInfoRequest[list.size()]));
        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        assertSubscriptionCompletion(testSubscriber, 2);
    }

//    @Test
//    public void getUserInfo4_ShouldFail_WithNullRequestArguments() throws Exception {
//        UserInfoRequest request = new UserInfoRequest(null, null, true);
//        List<UserInfoRequest> list = new ArrayList<>();
//        list.add(request);
//        list.add(request);
//        ExecutorService executor = apiClient.callExecutor();
//        mockExecutorSubmit(executor);
//
//        Observable<UserInfoResponse> observable = userApi.getUserInfo4(list.toArray(new UserInfoRequest[list.size()]));
//        TestSubscriber<UserInfoResponse> testSubscriber = new TestSubscriber<>();
//        observable.subscribe(testSubscriber);
//
//        assertSubscriptionError(testSubscriber, IllegalArgumentException.class);
//    }

    private void mockExecutorSubmit(ExecutorService executor) {
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            Runnable runnable = (Runnable) args[0];
            runnable.run();
            return new FutureTask<>(runnable, null);
        }).when(executor).submit(notNull(Runnable.class));
    }

    private void assertSubscriptionCompletion(TestSubscriber<UserInfoResponse> testSubscriber, int expectedValueCount) {
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertUnsubscribed();
        testSubscriber.assertValueCount(expectedValueCount);
    }

    private void assertSubscriptionError(TestSubscriber<UserInfoResponse> testSubscriber, Class<? extends Throwable> exceptionClass ) {
        testSubscriber.assertError(exceptionClass);
        testSubscriber.assertUnsubscribed();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
    }
}