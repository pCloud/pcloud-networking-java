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

import com.pcloud.networking.serialization.ParameterValue;

/**
 * A base implementation of a network response.
 * <p>
 * Contains the result code and the error message if any.
 */
public class ApiResponse {
    /**
     * The default result code for a successful request
     */
    public static final long RESULT_SUCCESS = 0;

    @ParameterValue("result")
    private long resultCode;

    @ParameterValue("error")
    private String message;

    @SuppressWarnings("unused")
    protected ApiResponse() {
        // Keep to allow instantiation during serialization.
    }

    /**
     * Creates the {@linkplain ApiResponse}.
     *
     * @param resultCode the result code of the response
     * @param message    the error message if present
     */
    public ApiResponse(long resultCode, String message) {
        this.resultCode = resultCode;
        this.message = message;
    }

    /**
     * Returns the error message for this {@linkplain ApiResponse}
     *
     * @return The error message for this {@linkplain ApiResponse}
     */
    public String message() {
        return message;
    }

    /**
     * Returns the result code for this {@linkplain ApiResponse}
     *
     * @return The result code for this {@linkplain ApiResponse}
     */
    public long resultCode() {
        return resultCode;
    }

    /**
     * Returns true if the request was successful, false otherwise
     *
     * @return true if the request was successful, false otherwise
     */
    public boolean isSuccessful() {
        return resultCode == RESULT_SUCCESS;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "result=" + resultCode +
                ", error='" + message + '\'' +
                '}';
    }
}
