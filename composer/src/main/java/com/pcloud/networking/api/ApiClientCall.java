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

import static com.pcloud.utils.IOUtils.closeQuietly;

import com.pcloud.networking.client.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ApiClientCall<T> implements Call<T> {

    private final ApiComposer apiComposer;
    private final com.pcloud.networking.client.Call rawCall;
    private final ResponseAdapter<T> responseAdapter;

    ApiClientCall(ApiComposer apiComposer,
                  com.pcloud.networking.client.Call rawCall,
                  ResponseAdapter<T> responseAdapter) {
        this.apiComposer = apiComposer;
        this.rawCall = rawCall;
        this.responseAdapter = responseAdapter;
    }

    @Override
    public String methodName() {
        return rawCall.request().methodName();
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

        rawCall.enqueue(new com.pcloud.networking.client.Callback() {
            @Override
            public void onFailure(com.pcloud.networking.client.Call call, IOException e) {
                if (!isCancelled()) callback.onFailure(ApiClientCall.this, e);
            }

            @Override
            public void onResponse(com.pcloud.networking.client.Call call, Response response) {
                boolean success = false;
                try {
                    T result = adapt(response);
                    if (!isCancelled()) {
                        callback.onResponse(ApiClientCall.this, result);
                        success = true;
                    }
                } catch (IOException e) {
                    if (!isCancelled()) callback.onFailure(ApiClientCall.this, e);
                } finally {
                    if (!success) closeQuietly(response);
                }
            }
        });
    }

    @Override
    public T enqueueAndWait() throws IOException, InterruptedException {
        return adapt(rawCall.enqueueAndWait());
    }

    @Override
    public T enqueueAndWait(long timeout, TimeUnit timeUnit)
            throws IOException, InterruptedException, TimeoutException {
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
    public boolean isCancelled() {
        return rawCall.isCancelled();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Call<T> clone() {
        return new ApiClientCall<>(apiComposer, rawCall.clone(), responseAdapter);
    }

    protected T adapt(Response response) throws IOException {
        boolean success = false;
        try {
            if (isCancelled()) {
                throw new IOException("Cancelled");
            }
            T result = responseAdapter.adapt(response);
            success = true;
            return result;
        } finally {
            if (!success) {
                closeQuietly(response);
            }
        }
    }
}
