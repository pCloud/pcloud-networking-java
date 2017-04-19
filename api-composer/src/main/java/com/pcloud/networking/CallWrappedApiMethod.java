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
import com.pcloud.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.reflect.Method;

class CallWrappedApiMethod<T> extends ApiMethod<T> {

    static final ApiMethod.Factory FACTORY = new Factory();

    public static class Factory extends ApiMethod.Factory {

        @Override
        public ApiMethod<?> create(ApiComposer composer, Method method, Class<?>[] argumentTypes, Annotation[][] argumentAnnotations) {
            Type returnType = method.getGenericReturnType();
            Type rawType = Types.getRawType(method.getReturnType());
            if (rawType != Call.class) {
                return null;
            }

            if (!(returnType instanceof ParameterizedType)) {
                throw new IllegalStateException("Call return type must be parameterized"
                        + " as Call<Foo> or Call<? extends Foo>");
            }

            Type innerType = Types.getParameterUpperBound(0, (ParameterizedType) returnType);
            Class<?> innerReturnType = Types.getRawType(innerType);
            if (!ApiResponse.class.isAssignableFrom(innerReturnType) &&
                    innerReturnType != ResponseBody.class) {
                return null;
            }

            return new Builder<>(composer, method, argumentAnnotations, innerReturnType).create();
        }

        static class Builder<T> {

            private String apiMethodName;

            private ArgumentAdapter<?>[] argumentAdapters;
            private TypeAdapter<T> returnTypeAdapter;
            private boolean hasDataParameter;

            private ApiComposer apiComposer;

            Builder(ApiComposer composer, Method method, Annotation[][] argumentAnnotations, Class<T> returnType) {
                this.apiComposer = composer;

                if (ApiResponse.class.isAssignableFrom(returnType)) {
                    returnTypeAdapter = apiComposer.transformer().getTypeAdapter(returnType);
                } else {
                    returnTypeAdapter = null;
                }

                apiMethodName = parseMethodNameAnnotation(method);
                Type[] parameterTypes = method.getParameterTypes();
                argumentAdapters = new ArgumentAdapter[parameterTypes.length];
                for (int index = 0; index < parameterTypes.length; index++) {
                    argumentAdapters[index] = resolveArgumentAdapter(method, parameterTypes[index], argumentAnnotations[index]);
                }
            }

            ApiMethod<?> create() {
                return new CallWrappedApiMethod<>(apiMethodName, argumentAdapters, returnTypeAdapter);
            }

            private ArgumentAdapter<?> resolveArgumentAdapter(Method method, Type parameterType, Annotation[] annotations) {
                Transformer transformer = apiComposer.transformer();
                for (Annotation annotation : annotations) {
                    Type annotationType = annotation.annotationType();
                    if (annotationType == com.pcloud.networking.RequestBody.class) {
                        TypeAdapter<?> typeAdapter = transformer.getTypeAdapter(parameterType);
                        //TODO: Add check if type adapter cannot be resolved
                        return ArgumentAdapters.requestBody(typeAdapter);
                    } else if (annotationType == Parameter.class) {
                        String name = ((Parameter) annotation).value();
                        if (name.equals("")) {
                            throw apiMethodError(method, "@Parameter-annotated methods cannot have an " +
                                    "empty parameter name.");
                        }
                        TypeAdapter<?> typeAdapter = transformer.getTypeAdapter(parameterType);
                        //TODO: Add check if type adapter cannot be resolved
                        return ArgumentAdapters.parameter(name, typeAdapter, parameterType);
                    } else if (annotationType == RequestData.class) {
                        if (hasDataParameter) {
                            throw apiMethodError(method, "API method cannot have more than one " +
                                    "parameter annotated with @RequestData");
                        }

                        hasDataParameter = true;
                        return ArgumentAdapters.dataSource();
                    }
                }

                throw apiMethodError(method, "Cannot adapt method parameter, missing @Parameter, @RequestBody or @RequestData annotation.");
            }
        }
    }

    private String apiMethodName;
    private ArgumentAdapter[] argumentAdapters;
    private TypeAdapter<T> returnTypeAdapter;

    CallWrappedApiMethod(String apiMethodName, ArgumentAdapter[] argumentAdapters, TypeAdapter<T> returnTypeAdapter) {
        this.apiMethodName = apiMethodName;
        this.argumentAdapters = argumentAdapters;
        this.returnTypeAdapter = returnTypeAdapter;
    }

    @Override
    public T invoke(ApiComposer apiComposer, Object[] args) throws IOException {
        com.pcloud.Call rawCall = apiComposer.apiClient()
                .newCall(convertToRequest(apiComposer.endpointProvider().endpoint(), apiMethodName, argumentAdapters, args));
        return (T) (returnTypeAdapter == null ?
                        new BodyApiClientCall(rawCall) :
                        new ApiClientCall<>(rawCall, returnTypeAdapter));
    }

    private static class BodyApiClientCall extends ApiClientCall<ResponseBody> {

        BodyApiClientCall(com.pcloud.Call rawCall) {
            super(rawCall, null);
        }

        @Override
        protected ResponseBody adapt(Response response) throws IOException {
            return response.responseBody();
        }
    }
}
