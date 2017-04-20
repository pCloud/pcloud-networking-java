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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class CallWrappedApiMethod<T> extends ApiMethod<Call<T>> {

    static final ApiMethod.Factory FACTORY = new Factory();

    private String apiMethodName;
    private RequestAdapter requestAdapter;
    private ResponseAdapter<T> returnTypeAdapter;

    private CallWrappedApiMethod(String apiMethodName, RequestAdapter requestAdapter, ResponseAdapter<T> returnTypeAdapter) {
        this.apiMethodName = apiMethodName;
        this.requestAdapter = requestAdapter;
        this.returnTypeAdapter = returnTypeAdapter;
    }

    @Override
    public Call<T> invoke(ApiComposer apiComposer, Object[] args) throws IOException {
        Request.Builder builder = Request.create()
                .endpoint(apiComposer.endpointProvider().endpoint())
                .methodName(apiMethodName);
        requestAdapter.adapt(builder, args);
        com.pcloud.Call rawCall = apiComposer.apiClient().newCall(builder.build());
        return new ApiClientCall<>(rawCall, returnTypeAdapter);
    }

    private static class Factory extends ApiMethod.Factory {

        @Override
        public ApiMethod<?> create(ApiComposer composer, Method method, Type[] argumentTypes, Annotation[][] argumentAnnotations) {
            Type returnType = method.getGenericReturnType();
            Type rawType = Types.getRawType(method.getReturnType());
            if (rawType != Call.class) {
                return null;
            }

            if (!(returnType instanceof ParameterizedType)) {
                throw new IllegalStateException("Call return type must be parameterized"
                        + " like Call<Foo> or Call<? extends Foo>");
            }

            Type innerType = Types.getParameterUpperBound(0, (ParameterizedType) returnType);
            Class<?> innerReturnType = Types.getRawType(innerType);
            if (!isAllowedResponseType(innerReturnType)) {
                return null;
            }

            String apiMethodName = parseMethodNameAnnotation(method);
            RequestAdapter requestAdapter = createRequestAdapter(composer, method, argumentTypes, argumentAnnotations);
            ResponseAdapter<?> returnTypeAdapter = getResponseAdapter(composer, method, innerReturnType);

            return new CallWrappedApiMethod<>(apiMethodName, requestAdapter, returnTypeAdapter);
        }
    }

}
