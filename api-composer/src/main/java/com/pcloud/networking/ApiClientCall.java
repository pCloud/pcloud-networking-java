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

import static com.pcloud.IOUtils.closeQuietly;

class ApiClientCall<T> implements Call<T> {

    private com.pcloud.Call rawCall;
    private TypeAdapter<T> responseAdapter;

    ApiClientCall(com.pcloud.Call rawCall, TypeAdapter<T> responseAdapter) {
        this.rawCall = rawCall;
        this.responseAdapter = responseAdapter;
    }

    @Override
    public T execute() throws IOException {
        Response response = rawCall.execute();
        return adapt(response);
    }

    @Override
    public void enqueue(final Callback<T> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback argument cannot be null.");
        }

        rawCall.enqueue(new com.pcloud.Callback() {
            @Override
            public void onFailure(com.pcloud.Call call, IOException e) {
                callback.onFailure(ApiClientCall.this, e);
            }

            @Override
            public void onResponse(com.pcloud.Call call, Response response) throws IOException {
                try {
                    callback.onResponse(ApiClientCall.this, adapt(response));
                } catch (IOException e) {
                    closeQuietly(response);
                    callback.onFailure(ApiClientCall.this, e);
                }
            }
        });
    }

    @Override
    public boolean isExecuted() {
        return rawCall.isExecuted();
    }

    @Override
    public void cancel() {
        rawCall.cancel();
    }

    @Override
    public boolean isCanceled() {
        return rawCall.isCancelled();
    }

    @Override
    public Call<T> clone() {
        return null;
    }

    protected T adapt(Response response) throws IOException{
        return responseAdapter.deserialize(response.responseBody().reader());
    }
}
