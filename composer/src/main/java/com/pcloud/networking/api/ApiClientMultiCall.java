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

package com.pcloud.networking.api;

import com.pcloud.networking.client.MultiResponse;
import com.pcloud.networking.client.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.pcloud.utils.IOUtils.closeQuietly;

class ApiClientMultiCall<T, R> implements MultiCall<T, R> {

    private ApiComposer apiComposer;
    private com.pcloud.networking.client.MultiCall rawCall;
    private ResponseAdapter<R> responseAdapter;
    private List<T> requests;

    ApiClientMultiCall(ApiComposer apiComposer,
                       com.pcloud.networking.client.MultiCall rawCall,
                       ResponseAdapter<R> responseAdapter,
                       List<T> requests) {
        this.apiComposer = apiComposer;
        this.rawCall = rawCall;
        this.responseAdapter = responseAdapter;
        this.requests = Collections.unmodifiableList(requests);
    }

    @Override
    public String methodName() {
        return rawCall.requests().get(0).methodName();
    }

    @Override
    public List<T> requests() {
        return requests;
    }

    @Override
    public List<R> execute() throws IOException {
        return adapt(rawCall.execute());
    }

    @Override
    public List<R> enqueueAndWait() throws IOException, InterruptedException {
        return adapt(rawCall.enqueueAndWait());
    }

    @Override
    public List<R> enqueueAndWait(long timeout, TimeUnit timeUnit)
            throws IOException, InterruptedException, TimeoutException {
        return adapt(rawCall.enqueueAndWait(timeout, timeUnit));
    }

    @Override
    public void enqueue(final MultiCallback<T, R> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback argument cannot be null.");
        }

        // Ensure that the "results" list will have valid [0, requests.size()) indexes.
        @SuppressWarnings("unchecked") final List<R> results = Arrays.asList((R[]) new Object[requests.size()]);

        rawCall.enqueue(new com.pcloud.networking.client.MultiCallback() {

            @Override
            public void onFailure(com.pcloud.networking.client.MultiCall call, IOException e,
                                  List<Response> completedResponses) {
                if (!isCancelled()) {
                    callback.onFailure(ApiClientMultiCall.this, e, Collections.unmodifiableList(results));
                }
            }

            @Override
            public void onResponse(com.pcloud.networking.client.MultiCall call, int key,
                                   Response response) {
                if (!isCancelled()) {
                    try {
                        R result = adapt(response);
                        results.set(key, result);
                        callback.onResponse(ApiClientMultiCall.this, key, result);
                    } catch (IOException e) {
                        callback.onFailure(ApiClientMultiCall.this, e, results);
                        // Cancel the wrapped MultiCall to stop receiving other responses.
                        rawCall.cancel();
                    }
                }
            }

            @Override
            public void onComplete(com.pcloud.networking.client.MultiCall call,
                                   MultiResponse response) {
                if (!isCancelled()) {
                    callback.onComplete(ApiClientMultiCall.this, Collections.unmodifiableList(results));
                }
            }
        });
    }

    @Override
    public Interactor<R> start() {
        final com.pcloud.networking.client.Interactor rawInteractor = rawCall.start();
        return new Interactor<R>() {

            @Override
            public boolean hasMoreRequests() {
                return rawInteractor.hasMoreRequests();
            }

            @Override
            public int submitRequests(int count) throws IOException {
                return rawInteractor.submitRequests(count);
            }

            @Override
            public boolean hasNextResponse() {
                return rawInteractor.hasNextResponse();
            }

            @Override
            public R nextResponse() throws IOException {
                return adapt(rawInteractor.nextResponse());
            }

            @Override
            public void close() {
                rawInteractor.close();
            }
        };
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

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public MultiCall<T, R> clone() {
        return new ApiClientMultiCall<>(apiComposer, rawCall.clone(), responseAdapter, requests);
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

    private R adapt(Response response) throws IOException {
        if (isCancelled()) {
            throw new IOException("Cancelled");
        }
        R result = responseAdapter.adapt(response);

        if (result instanceof ApiResponse) {
            List<ResponseInterceptor> interceptors = apiComposer.interceptors();
            for (ResponseInterceptor interceptor : interceptors) {
                try {
                    interceptor.intercept((ApiResponse) result);
                } catch (Exception e) {
                    closeQuietly(response);
                    throw new RuntimeException(
                            String.format("Error while calling ResponseInterceptor of type '%s' for '%s' call.",
                                    interceptor.getClass(), methodName()), e
                    );
                }
            }
        }
        return result;
    }
}
