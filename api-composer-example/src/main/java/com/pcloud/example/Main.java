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

package com.pcloud.example;

import com.pcloud.PCloudAPIClient;
import com.pcloud.Request;
import com.pcloud.RequestInterceptor;
import com.pcloud.networking.ApiComposer;
import com.pcloud.networking.Transformer;
import com.pcloud.protocol.streaming.ProtocolWriter;
import com.pcloud.protocol.streaming.TypeToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String... args) {
        PCloudAPIClient apiClient = PCloudAPIClient.newClient()
                .addInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(Request request, ProtocolWriter writer) throws IOException {
                        writer.writeName("timeformat", TypeToken.STRING).writeValue("timestamp");
                    }
                })
                .create();
        Transformer transformer = Transformer.create().build();

        try {
            ApiComposer apiComposer = ApiComposer.create()
                    .apiClient(apiClient)
                    .transformer(transformer)
                    .loadEagerly(true)
                    .create();

            UserApi userApi = apiComposer.compose(UserApi.class);

            String username = System.getenv("pcloud_username");
            String password = System.getenv("pcloud_password");
            UserInfoRequest request = new UserInfoRequest(username, password, true);
            UserInfoResponse response = userApi.getUserInfo(username, password, true);
            UserInfoResponse response1 = userApi.getUserInfo1(request);
            UserInfoResponse response2 = userApi.getUserInfo2(username, password, true).execute();
            UserInfoResponse response3 = userApi.getUserInfo3(request).execute();

            UserInfoRequest[] requests = new UserInfoRequest[10];
            Arrays.fill(requests, request);
            List<UserInfoResponse> response4 = userApi.getUserInfo4(requests).execute();
            List<UserInfoResponse> response5 = userApi.getUserInfo5(Arrays.asList(requests)).execute();
            System.out.println();
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            apiClient.shutdown();
        }
    }
}
