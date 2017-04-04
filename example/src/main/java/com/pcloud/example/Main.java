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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            String token = getAuthToken(client, cyclone, username, password);

            boolean requestMoreDiffs;
            long lastDiffId = 0;
            final int chunkSize = 50000;
            long start = System.currentTimeMillis();
            long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long currentMemory = startMemory;
            List<DiffResultResponse.DiffEntry> entries = new LinkedList<>();
            do {
                DiffResultResponse resultResponse = getDiffs(client, cyclone, token, lastDiffId, chunkSize);
                lastDiffId = resultResponse.diffId();
                currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                System.gc();
                System.out.println("Read " +resultResponse.entries().size() + " diffs. Last diff id "+lastDiffId
                        +" memory grew with "+(currentMemory - startMemory)/1024+" kB.");
                entries.addAll(resultResponse.entries());
                requestMoreDiffs = resultResponse.entries().size() == chunkSize;
            } while (requestMoreDiffs);
            System.gc();
            currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.out.println(entries.size() + " diffs total for "+ TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)+
                    "s, used memory grew with "+(currentMemory - startMemory)/1024+" kB.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    private static String getAuthToken(PCloudAPIClient client, Cyclone cyclone, String username, String password) throws IOException {
        Map<String, Object> values = new HashMap<>();
        values.put("getauth", 1);
        values.put("username", username);
        values.put("password", password);

        Response response = null;
        try {
            response = client.newCall(Request.create()
                    .methodName("userinfo")
                    .body(RequestBody.fromValues(values))
                    .build())
                    .execute();
            UserInfoResponse apiResponse = cyclone.getTypeAdapter(UserInfoResponse.class)
                    .deserialize(response.responseBody().reader());
            return apiResponse.authenticationToken();
        } finally {
            closeQuietly(response);
        }
    }

    private static DiffResultResponse getDiffs(PCloudAPIClient client, Cyclone cyclone, String token,long lastDiffId, int chunkSize) throws IOException {
        Map<String, Object> values = new HashMap<>();
        values.put("auth", token);
        values.put("difflimit", chunkSize);
        values.put("diffid", lastDiffId);
        values.put("subscribefor", "diff");

        Response response = null;
        try {
            response = client.newCall(Request.create()
                    .methodName("subscribe")
                    .body(RequestBody.fromValues(values))
                    .build()).execute();
            return cyclone.getTypeAdapter(DiffResultResponse.class).deserialize(response.responseBody().reader());
        } finally {
            closeQuietly(response);
        }
    }
}
