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

import java.io.IOException;

class DatApiClientCall extends ApiClientCall<DataApiResponse> {

    DatApiClientCall(com.pcloud.Call rawCall, TypeAdapter<DataApiResponse> responseAdapter) {
        super(rawCall, responseAdapter);
    }

    @Override
    protected DataApiResponse adapt(Response response) throws IOException {
        DataApiResponse result = super.adapt(response);
        if (result.isSuccessful()) {
            result = new DataApiResponse(result.resultCode(), result.message(), response.responseBody().data());
        }

        return result;
    }
}
