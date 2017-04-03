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

import com.pcloud.PCloudAPIClient;
import com.pcloud.Request;
import com.pcloud.RequestBody;
import com.pcloud.Response;
import com.pcloud.networking.Cyclone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.pcloud.IOUtils.closeQuietly;

public class Main {

    public static void main(String... args) {

        final String username = System.getenv("pcloud_username");
        final String password = System.getenv("pcloud_password");

        Cyclone cyclone = Cyclone.create()
                .build();

        PCloudAPIClient client = PCloudAPIClient.newClient()
                .create();
        try {
            String token = getAuthToken(client, username, password);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    private static String getAuthToken(PCloudAPIClient client, String username, String password) throws IOException {
        Map<String, Object> values = new HashMap<>();
        values.put("getauth", 1);
        values.put("username", username);
        values.put("password", password);

        Response response = null;
        try {
            response = client.newCall(Request.create()
                    .methodName("userinfo")
                    .body(RequestBody.fromValues(values))
                    .build()).execute();
            Map<String, ?> responseValues = response.responseBody().toValues();
            return (String) responseValues.get("auth");
        } finally {
            closeQuietly(response);
        }
    }
}
