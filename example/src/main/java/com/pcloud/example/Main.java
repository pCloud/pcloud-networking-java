/*
 * Copyright (c) 2016 Georgi Neykov
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

package com.pcloud.example;

import com.pcloud.Authenticator;
import com.pcloud.PCloudAPIClient;
import com.pcloud.Request;
import com.pcloud.Response;

import java.io.IOException;

public class Main {

    public static void main(String... args) {

        final String username = System.getenv("pcloud_username");
        final String password = System.getenv("pcloud_password");

        PCloudAPIClient client = PCloudAPIClient.newClient()
                .setAuthenticator(new Authenticator() {
                    public Request authenticate(Request request) {
                        /*TODO: Add auth parameters to request here.*/
                        request.newRequest().addParameter("username", username)
                                .addParameter("password", password);
                        return request;
                    }
                })
                .create();
        try {
            Response response = login(client, username, password);
            if (response.isSuccessful()) {
                listFiles(client, "/Automatic Upload", false);
            } else {
                System.out.println("Failed to login with the provided credentials.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    private static Response listFiles(PCloudAPIClient client, String path, boolean recursive) throws IOException {
        Request request = Request.create()
                .methodName("listfolder")
                .addParameter("timeformat","timestamp")
                .addParameter("path", path)
                .addParameter("recursive", recursive)
                .build();

        System.out.println(request);
        Response filesResponse = client.execute(request);
        System.out.println(filesResponse);
        return filesResponse;
    }

    private static Response login(PCloudAPIClient client, String username, String password) throws IOException {
        Request loginRequest =
                Request.create()
                        .methodName("userinfo")
                        .addParameter("getauth", 1)
                        .addParameter("username", username)
                        .addParameter("password", password)
                        .build();
        System.out.println(loginRequest);
        Response loginResponse = client.execute(loginRequest);
        System.out.println(loginResponse);
        return loginResponse;
    }
}
