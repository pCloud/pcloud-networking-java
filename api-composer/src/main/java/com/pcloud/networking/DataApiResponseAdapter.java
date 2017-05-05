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

import com.pcloud.Response;
import com.pcloud.ResponseData;

import java.io.IOException;

import static com.pcloud.IOUtils.closeQuietly;

class DataApiResponseAdapter<T> implements ResponseAdapter<T> {

    private TypeAdapter<? extends DataApiResponse> typeAdapter;

    public DataApiResponseAdapter(TypeAdapter<? extends DataApiResponse> typeAdapter) {
        this.typeAdapter = typeAdapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T adapt(Response response) throws IOException {
        boolean success = false;
        try {
            DataApiResponse result = typeAdapter.deserialize(response.responseBody().reader());
            ResponseData data = result.isSuccessful() ? response.responseBody().data() : null;
            result.setResponseData(data);
            success = true;
            return (T) result;
        } finally {
            if (!success) {
                closeQuietly(response);
            }
        }
    }
}
