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

import com.pcloud.networking.client.*;
import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.serialization.Transformer;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.pcloud.utils.IOUtils.closeQuietly;
import static org.mockito.Mockito.spy;

public abstract class ApiIntegrationTest {

    protected static PCloudAPIClient apiClient;
    protected static Transformer transformer;
    protected static ApiComposer apiComposer;
    protected static FileOperationApi donwloadApi;
    protected static FolderApi folderApi;
    protected static UserApi userApi;

    protected static String username;
    protected static String password;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws Exception {
        synchronized (ApiIntegrationTest.class) {
            resolveTestAccountCredentials();

            apiClient = PCloudAPIClient.newClient()
                    .callExecutor(spy(new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                            new SynchronousQueue<Runnable>(), new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "PCloud API Client");
                        }
                    })))
                    .addInterceptor(new RequestInterceptor() {
                        @Override
                        public void intercept(Request request, ProtocolWriter writer) throws IOException {
                            writer.writeName("timeformat").writeValue("timestamp");
                        }
                    })
                    .create();
            transformer = Transformer.create().build();

            final String token = getAuthToken(apiClient, transformer, username, password);
            apiClient = apiClient.newBuilder().addInterceptor(new RequestInterceptor() {
                @Override
                public void intercept(Request request, ProtocolWriter writer) throws IOException {
                    writer.writeName("auth").writeValue(token);
                }
            }).create();

            apiComposer = ApiComposer.create()
                    .apiClient(apiClient)
                    .transformer(transformer)
                    .loadEagerly(true)
                    .create();

            donwloadApi = apiComposer.compose(FileOperationApi.class);
            folderApi = apiComposer.compose(FolderApi.class);
            userApi = apiComposer.compose(UserApi.class);
        }
    }

    private static void resolveTestAccountCredentials() {
        Map<String, String> env = System.getenv();
        username = env.get("PCLOUD_TEST_USERNAME");
        password = env.get("PCLOUD_TEST_PASSWORD");

        if (username == null) {
            throw new IllegalStateException("'PCLOUD_TEST_USERNAME' environment variable not set.");
        }

        if (password == null) {
            throw new IllegalStateException("'PCLOUD_TEST_PASSWORD' environment variable not set.");
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
                    .body(com.pcloud.networking.client.RequestBody.fromValues(values))
                    .build())
                    .enqueueAndWait();
            UserInfoResponse apiResponse = transformer.getTypeAdapter(UserInfoResponse.class)
                    .deserialize(response.responseBody().reader());
            if (!apiResponse.isSuccessful()){
                throw new IOException(apiResponse.resultCode()+"-"+apiResponse.message());
            }
            return apiResponse.authenticationToken();
        } finally {
            closeQuietly(response);
        }
    }
}
