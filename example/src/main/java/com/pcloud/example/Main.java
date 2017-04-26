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

package com.pcloud.example;

import com.pcloud.*;
import com.pcloud.networking.Transformer;
import com.pcloud.protocol.streaming.BytesReader;
import com.pcloud.protocol.streaming.ProtocolReader;
import com.pcloud.protocol.streaming.ProtocolWriter;
import com.pcloud.protocol.streaming.TypeToken;
import okio.ByteString;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.pcloud.IOUtils.closeQuietly;

public class Main {


    public static void main(String... args) {

        final String username = System.getenv("pcloud_username");
        final String password = System.getenv("pcloud_password");

        Transformer transformer = Transformer.create()
                .build();



        RequestInterceptor dateFormatInterceptor = (request, writer) -> {
                writer.writeName("timeformat").writeValue("timestamp");
            };

        PCloudAPIClient client = PCloudAPIClient.newClient()
                .addInterceptor(dateFormatInterceptor)
                .create();
        try {
            String token = getAuthToken(client, transformer, username, password);
            client = client.newBuilder().addInterceptor((request, writer) -> {
                writer.writeName("auth").writeValue(token);
            }).create();
           // pullDiffs(transformer, client, token);
           // getMultiThumb(client, token, 2516863197L);
            /*getChecksums(client,
                    2516863197L, 2516863303L,
                    2516863074L, 2516862988L,
                    2516869906L, 2516869939L,
                    2516869733L, 2516869808L,
                    2516869566L, 2516869479L);*/
            getThumb(client, 2516863197L);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }

    private static void pullDiffs(Transformer transformer, PCloudAPIClient client, String token) throws IOException {
        boolean requestMoreDiffs;
        long lastDiffId = 0;
        final int chunkSize = 50000;
        long start = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long currentMemory = startMemory;
        List<DiffResultResponse.DiffEntry> entries = new LinkedList<>();
        do {
            DiffResultResponse resultResponse = getDiffs(client, transformer, lastDiffId, chunkSize);
            lastDiffId = resultResponse.diffId();
            currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.gc();
            System.out.println("Read " + resultResponse.entries().size() + " diffs. Last diff id " + lastDiffId
                    + " memory grew with " + (currentMemory - startMemory) / 1024 + " kB.");
            entries.addAll(resultResponse.entries());
            requestMoreDiffs = resultResponse.entries().size() == chunkSize;
        } while (requestMoreDiffs);
        System.gc();
        currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println(entries.size() + " diffs total for " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) +
                "s, used memory grew with " + (currentMemory - startMemory) / 1024 + " kB.");
    }

    private static void getMultiThumb(PCloudAPIClient cloudAPIClient, String token, long fileId) throws IOException {
        Map<String, Object> values = new TreeMap<>();
        values.put("auth", token);
        values.put("fileid", fileId);
        values.put("size", "128x128");

        MultiResponse response = null;
        try {
            List<Request> requests = new ArrayList<>();
            for (int i = 0; i < 1; i++){
                requests.add(Request.create()
                        .body(RequestBody.fromValues(values))
                        .methodName("getthumb")
                        .build());
            }
            response = cloudAPIClient.newCall(requests)
                    .execute();

        } finally {
            closeQuietly(response);
        }
    }

    private static void getChecksums(PCloudAPIClient cloudAPIClient, long... fileids) throws IOException {
        MultiResponse response = null;
        try {
            List<Request> requests = new ArrayList<>();
            for (int i = 0; i < fileids.length; i++){
                Map<String, Object> values = new TreeMap<>();
                values.put("fileid", fileids[i]);
                requests.add(Request.create()
                        .body(RequestBody.fromValues(values))
                        .methodName("getthumb")
                        .build());
            }
            response = cloudAPIClient.newCall(requests)
                    .enqueueAndWait();
            response.toString();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(response);
        }
    }

    private static void getThumb(PCloudAPIClient cloudAPIClient, long fileId) throws IOException, InterruptedException {
        Map<String, Object> values = new TreeMap<>();
        values.put("path", "/cat.jpg");
        values.put("size", "32x32");

        Response response = null;
        try {
            response = cloudAPIClient.newCall(
                    Request.create()
                            .body(RequestBody.fromValues(values))
                            .methodName("getthumb")
                            .build())
                    .enqueueAndWait();

            Map<String, ?> responseValues = response.responseBody().toValues();
            ResponseData data = response.responseBody().data();
            responseValues.toString();
        } finally {
            closeQuietly(response);
        }
    }

    private static String getAuthToken(PCloudAPIClient client, Transformer transformer, String username, String password) throws IOException, InterruptedException {
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
                    .enqueueAndWait();
            UserInfoResponse apiResponse = transformer.getTypeAdapter(UserInfoResponse.class)
                    .deserialize(response.responseBody().reader());
            return apiResponse.authenticationToken();
        } finally {
            closeQuietly(response);
        }
    }

    private static DiffResultResponse getDiffs(PCloudAPIClient client, Transformer transformer, long lastDiffId, int chunkSize) throws IOException {
        Map<String, Object> values = new HashMap<>();
        values.put("difflimit", chunkSize);
        values.put("diffid", lastDiffId);
        values.put("subscribefor", "diff");

        DiffRequest request = new DiffRequest(60, lastDiffId, chunkSize);

        Response response = null;
        try {
            response = client.newCall(Request.create()
                    .methodName("diff")
                    .body(new RequestBody() {
                        @Override
                        public void writeТо(ProtocolWriter writer) throws IOException {
                            transformer.getTypeAdapter(DiffRequest.class).serialize(writer, request);
                        }
                    })
                    .build())
                    .execute();
            return transformer.getTypeAdapter(DiffResultResponse.class).deserialize(response.responseBody().reader());
        } finally {
            closeQuietly(response);
        }
    }
}
