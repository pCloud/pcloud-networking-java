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

import com.pcloud.Request;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class CallWrappedApiMethod<T, R> extends ApiMethod<R> {

    static final ApiMethod.Factory FACTORY = new Factory();

    private String apiMethodName;
    private RequestAdapter requestAdapter;
    private ResponseAdapter<T> returnTypeAdapter;
    private CallAdapter callAdapter;

    private CallWrappedApiMethod(String apiMethodName, RequestAdapter requestAdapter,
                                 ResponseAdapter<T> returnTypeAdapter, CallAdapter callAdapter) {
        this.apiMethodName = apiMethodName;
        this.requestAdapter = requestAdapter;
        this.returnTypeAdapter = returnTypeAdapter;
        this.callAdapter = callAdapter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R invoke(ApiComposer apiComposer, Object[] args) throws IOException {
        Request.Builder builder = Request.create()
                                    .endpoint(apiComposer.endpointProvider().endpoint())
                                    .methodName(apiMethodName);
        requestAdapter.adapt(builder, args);
        com.pcloud.Call rawCall = apiComposer.apiClient().newCall(builder.build());

        return (R) callAdapter.adapt(new ApiClientCall(apiComposer, rawCall, returnTypeAdapter));
    }

    private static class Factory extends ApiMethod.Factory {

        @Override
        public ApiMethod<?> create(ApiComposer composer, Method method,
                                   Type[] argumentTypes, Annotation[][] argumentAnnotations) {
            String apiMethodName = parseMethodNameAnnotation(method);
            RequestAdapter requestAdapter = getRequestAdapter(composer, method, argumentTypes, argumentAnnotations);

            CallAdapter<?, ?> callAdapter = composer.loadCallAdapter(method);
            if (callAdapter == null) {
                return null;
            }
            Type adapterResponseType = callAdapter.responseType();

            if (!isAllowedResponseType(adapterResponseType)) {
                return null;
            }

            ResponseAdapter<?> returnTypeAdapter = getResponseAdapter(composer, method, adapterResponseType);

            return new CallWrappedApiMethod<>(apiMethodName, requestAdapter, returnTypeAdapter, callAdapter);
        }
    }
}
