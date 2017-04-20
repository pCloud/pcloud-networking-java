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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ApiClientCall<T> implements Call<T> {

    private com.pcloud.Call rawCall;
    private ResponseAdapter<T> responseAdapter;

    ApiClientCall(com.pcloud.Call rawCall, ResponseAdapter<T> responseAdapter) {
        this.rawCall = rawCall;
        this.responseAdapter = responseAdapter;
    }

    @Override
    public T execute() throws IOException {
        return adapt(rawCall.execute());
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
                    callback.onFailure(ApiClientCall.this, e);
                }
            }
        });
    }

    @Override
    public T enqueueAndWait() throws IOException, InterruptedException {
        return adapt(rawCall.enqueueAndWait());
    }

    @Override
    public T enqueueAndWait(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException, TimeoutException {
        return adapt(rawCall.enqueueAndWait(timeout, timeUnit));
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

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public Call<T> clone() {
        return new ApiClientCall<>(rawCall, responseAdapter);
    }

    protected T adapt(Response response) throws IOException {
        return responseAdapter.adapt(response);
    }
}
