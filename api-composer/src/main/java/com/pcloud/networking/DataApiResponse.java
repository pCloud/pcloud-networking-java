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

import com.pcloud.ResponseData;

import java.io.Closeable;

public class DataApiResponse extends ApiResponse implements Closeable{

    private ResponseData responseData;

    protected DataApiResponse() {
    }

    public DataApiResponse(long resultCode, String message) {
        super(resultCode, message);
    }

    /**
     * Injects a {@linkplain ResponseData} object to this instance.
     * <p>
     * <b>NOTE:</b> Should be called only once per instance.
     * @param data data object to be injected, can be null.
     */
    final void setResponseData(ResponseData data){
        synchronized (this) {
            if (this.responseData != null) {
                throw new AssertionError("ResponseData already set to instance.");
            }
            this.responseData = data;
        }
    }

    public ResponseData responseData() {
        return responseData;
    }

    @Override
    public void close() {
        responseData.close();
    }
}
