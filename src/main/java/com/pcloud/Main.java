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

package com.pcloud;

import com.pcloud.value.ObjectValue;

import java.io.IOException;

public class Main {

    public static void main(String... args) {

        ApiConnector connector = new ApiConnector();
        try {
            listFiles(connector, "/", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ApiResponse listFiles(ApiConnector connector, String path, boolean recursive) throws IOException {
        ObjectValue listFilesRequest = new ObjectValue();
        listFilesRequest.putString("path", path);
        listFilesRequest.putNumber("recursive", recursive ? 1 : 0);
        System.out.println(listFilesRequest);
        ApiResponse filesResponse = connector.callMethod("listfolder", listFilesRequest);
        System.out.println(filesResponse);
        return filesResponse;
    }

    private static ApiResponse login(ApiConnector connector, String username, String password) throws IOException {
        ObjectValue loginRequest = new ObjectValue();
        loginRequest.putString("username", username);
        loginRequest.putString("password", password);
        System.out.println(loginRequest);
        ApiResponse loginResponse = connector.callMethod("userinfo", loginRequest);
        System.out.println(loginResponse);
        return loginResponse;
    }
}
