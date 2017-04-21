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

package com.pcloud.networking;

public class ApiResponse {
    public static final int RESULT_SUCCESS = 0;

    @ParameterValue("result")
    private long resultCode;

    @ParameterValue("error")
    private String message;

    @SuppressWarnings("unused")
    protected ApiResponse() {
        // Keep to allow instantiation during serialization.
    }

    public ApiResponse(long resultCode, String message) {
        this.resultCode = resultCode;
        this.message = message;
    }

    public String message() {
        return message;
    }

    public long resultCode() {
        return resultCode;
    }

    public boolean isSuccessful() {
        return resultCode == RESULT_SUCCESS;
    }
}
