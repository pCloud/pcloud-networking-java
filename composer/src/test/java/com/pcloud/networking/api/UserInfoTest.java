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

package com.pcloud.networking.api;

import com.pcloud.networking.client.Endpoint;
import com.pcloud.networking.client.ResponseBody;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UserInfoTest extends ApiIntegrationTest {

    @Test
    public void getUserInfo_ShouldNotFail() throws Exception {

        UserInfoResponse response = userApi.getUserInfo(username, password, true);
        assertNotNull(response);
    }

    @Test
    public void getUserInfo_ShouldNotFailOnNullParameters() throws Exception {
        userApi.getUserInfo(null, "", true);
    }

    @Test
    public void getUserInfo1_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        UserInfoResponse response = userApi.getUserInfo1(request);
        assertNotNull(response);
    }

    @Test
    public void getUserInfo1_ShouldFailOnNullRequestObject() throws Exception {

        exception.expect(IllegalArgumentException.class);
        userApi.getUserInfo1(null);
    }

    @Test
    public void getUserInfo2_ShouldNotFail() throws Exception {

        Call<UserInfoResponse> call = userApi.getUserInfo2(username, password, true);
        UserInfoResponse response = call.execute();
        assertNotNull(response);
    }

    @Test
    public void getUserInfo2_ShouldNotFailOnNullParameters() throws Exception {
        Call<UserInfoResponse> call = userApi.getUserInfo2(null, null, false);
        call.execute();
    }

    @Test
    public void getUserInfo3_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        Call<UserInfoResponse> call = userApi.getUserInfo3(request);
        UserInfoResponse response = call.execute();
        assertNotNull(response);
    }

    @Test
    public void getUserInfo3_ShouldFail_WhenRequestIsNull() throws Exception {

        exception.expect(IllegalArgumentException.class);
        Call<UserInfoResponse> call = userApi.getUserInfo3(null);
        call.execute();
    }

    @Test
    public void getUserInfo4_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        MultiCall<UserInfoRequest, UserInfoResponse> call = userApi.getUserInfo4(request);
        List<UserInfoResponse> list = call.execute();
        assertNotNull(list);
        assertNotNull(list.get(0));
    }

    @Test
    public void getUserInfo4_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        userApi.getUserInfo4((UserInfoRequest[]) null);
    }

    @Test
    public void getUserInfo5_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        List<UserInfoRequest> list = new ArrayList<>();
        list.add(request);
        MultiCall<UserInfoRequest, UserInfoResponse> call = userApi.getUserInfo5(list);
        List<UserInfoResponse> response = call.execute();
        assertNotNull(response);
        assertNotNull(response.get(0));
    }

    @Test
    public void getUserInfo5_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        MultiCall<UserInfoRequest, UserInfoResponse> call = userApi.getUserInfo5(null);
        call.execute();
    }

    @Test
    public void getUserInfoOnEndpoint_Should_Execute_Call_OnProvided_Endpoint() throws Exception {
        Endpoint expected = new Endpoint("binapiams1.pcloud.com", 443);

        try (ResponseBody body = userApi.getUserInfoOnEndpoint(expected).execute()) {
            assertEquals(expected, body.endpoint());
        }
    }
}