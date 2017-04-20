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

package com.pcloud;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dimitard 20.4.2017 г..
 */
public class RequestUtils {

    private static final String MOCK_USERNAME = "mockuser@qa.mobileinno.com";
    private static final String MOCK_PASSWORD = "mockpass";

    public static Request getUserInfoRequest(Endpoint endpoint) {
        Map<String, Object> values = new HashMap<>();
        values.put("getauth", 1);
        values.put("username", MOCK_USERNAME);
        values.put("password", MOCK_PASSWORD);
        return Request.create()
                .methodName("userinfo")
                .body(RequestBody.fromValues(values))
                .endpoint(endpoint)
                .build();
    }
}
