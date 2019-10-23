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

import java.io.IOException;
import java.util.List;

public interface UserApi {

    @Method("userinfo")
    UserInfoResponse getUserInfo(@Parameter("username")String username, @Parameter("password")String password, @Parameter("getauth") boolean returnToken) throws IOException;

    @Method("userinfo")
    UserInfoResponse getUserInfo1(@RequestBody UserInfoRequest request) throws IOException;

    @Method("userinfo")
    Call<UserInfoResponse> getUserInfo(@Parameter("auth") String token, @Parameter("getauth") boolean returnToken);

    @Method("userinfo")
    Call<UserInfoResponse> getUserInfo2(@Parameter("username")String username, @Parameter("password")String password, @Parameter("getauth") boolean returnToken);

    @Method("userinfo")
    Call<UserInfoResponse> getUserInfo3(@RequestBody UserInfoRequest request);

    @Method("userinfo")
    Call<ResponseBody> getUserInfoOnEndpoint(Endpoint endpoint);

    @Method("userinfo")
    MultiCall<UserInfoRequest, UserInfoResponse> getUserInfo4(@RequestBody UserInfoRequest... request);

    @Method("userinfo")
    MultiCall<UserInfoRequest, UserInfoResponse> getUserInfo5(@RequestBody List<UserInfoRequest> request);

    @Method("logout")
    Call<ApiResponse> logout(@Parameter("auth") String token);
}
