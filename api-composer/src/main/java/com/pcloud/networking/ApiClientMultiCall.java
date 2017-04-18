/*
 * Copyright (C) 2017 pCloud AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.networking;

import com.pcloud.MultiResponse;
import com.pcloud.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.pcloud.IOUtils.closeQuietly;

public class ApiClientMultiCall<T, R> implements MultiCall<T, R> {

    private com.pcloud.MultiCall rawCall;
    private TypeAdapter<R> responseAdapter;
    private List<T> requests;

    ApiClientMultiCall(com.pcloud.MultiCall rawCall, TypeAdapter<R> responseAdapter, List<T> requests) {
        this.rawCall = rawCall;
        this.responseAdapter = responseAdapter;
        this.requests = Collections.unmodifiableList(requests);
    }

    @Override
    public List<T> requests() {
        return requests;
    }

    @Override
    public List<R> execute() throws IOException {
        return adapt(rawCall.execute());
    }

    private List<R> adapt(MultiResponse multiResponse) throws IOException {
        try {
            int responseCount = multiResponse.responses().size();
            List<R> responses = new ArrayList<>(responseCount);
            for (Response response : multiResponse.responses()) {
                responses.add(adapt(response));
            }
            return responses;
        } finally {
            closeQuietly(multiResponse);
        }
    }

    @Override
    public List<R> enqueueAndWait() throws IOException, InterruptedException {
        return adapt(rawCall.enqueueAndWait());
    }

    @Override
    public List<R> enqueueAndWait(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException, TimeoutException {
        return adapt(rawCall.enqueueAndWait(timeout, timeUnit));
    }

    @Override
    public void enqueue(final MultiCallback<T, R> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback argument cannot be null.");
        }

        final int responseCount = requests.size();
        rawCall.enqueue(new com.pcloud.MultiCallback() {


            List<R> results = new ArrayList<>(responseCount);

            @Override
            public void onFailure(com.pcloud.MultiCall call, IOException e, List<Response> completedResponses) {
                callback.onFailure(ApiClientMultiCall.this, e, Collections.unmodifiableList(results));
            }

            @Override
            public void onResponse(com.pcloud.MultiCall call, int key, Response response) throws IOException {
                if (results.isEmpty()) {
                    for (int i = 0; i < responseCount; i++){
                        results.add(null);
                    }
                }
                R result = adapt(response);
                results.set(key, result);
                callback.onResponse(ApiClientMultiCall.this, key, result);
            }

            @Override
            public void onComplete(com.pcloud.MultiCall call, MultiResponse response) throws IOException {
                callback.onComplete(ApiClientMultiCall.this, Collections.unmodifiableList(results));
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
    public boolean isCancelled() {
        return rawCall.isCancelled();
    }

    private R adapt(Response response) throws IOException {
        return responseAdapter.deserialize(response.responseBody().reader());
    }
}
