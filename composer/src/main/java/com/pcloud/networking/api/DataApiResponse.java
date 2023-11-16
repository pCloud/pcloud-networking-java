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

import com.pcloud.networking.client.ResponseData;

import java.io.Closeable;

import static com.pcloud.utils.IOUtils.closeQuietly;

/**
 * An implementation to house the {@linkplain ResponseData} for the response of a network call
 *
 * @see ApiResponse
 * @see ResponseData
 */

@SuppressWarnings("unused")
public class DataApiResponse extends ApiResponse implements Closeable {

    private ResponseData responseData;

    protected DataApiResponse() {
    }

    /**
     * Creates the {@linkplain DataApiResponse}
     *
     * @param resultCode The result code of the request
     * @param message    The error message if present
     */
    public DataApiResponse(long resultCode, String message) {
        super(resultCode, message);
    }

    /**
     * Injects a {@linkplain ResponseData} object to this instance.
     * <p>
     * <b>NOTE:</b> Should be called only once per instance.
     *
     * @param data data object to be injected, can be null.
     */
    final void setResponseData(ResponseData data) {
        if (this.responseData != null) {
            throw new IllegalStateException("ResponseData already set to instance.");
        }
        this.responseData = data;
    }

    /**
     * Returns the {@linkplain ResponseData} of this response
     *
     * @return The {@linkplain ResponseData} of this response
     */
    public ResponseData responseData() {
        return responseData;
    }

    @Override
    public void close() {
        closeQuietly(responseData);
    }
}
