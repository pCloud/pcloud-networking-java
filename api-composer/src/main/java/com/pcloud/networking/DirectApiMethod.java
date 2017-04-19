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
import com.pcloud.Response;
import com.pcloud.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static com.pcloud.IOUtils.closeQuietly;

class DirectApiMethod<T> extends ApiMethod<T> {

    static final ApiMethod.Factory FACTORY = new Factory();

    static class Factory extends ApiMethod.Factory {

        @Override
        public ApiMethod<?> create(ApiComposer composer, Method method, Class<?>[] argumentTypes, Annotation[][] argumentAnnotations) {
            Class<?> returnType = Types.getRawType(method.getReturnType());
            if (!ApiResponse.class.isAssignableFrom(returnType) && returnType != ResponseBody.class) {
                return null;
            }

            return new Builder<>(composer, method, argumentAnnotations, returnType).create();
        }

        static class Builder<T> {

            private String apiMethodName;

            private ArgumentAdapter<?>[] argumentAdapters;
            private TypeAdapter<T> returnTypeAdapter;
            private boolean hasDataParameter;

            private ApiComposer apiComposer;
            private Class<T> returnType;

            Builder(ApiComposer composer, Method method, Annotation[][] argumentAnnotations, Class<T> returnType) {
                this.apiComposer = composer;
                this.returnType = returnType;

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

            com.pcloud.networking.ApiMethod<T> create() {
                return new DirectApiMethod<>(returnType, apiMethodName, argumentAdapters, returnTypeAdapter);
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
    private Class<T> returnType;

    private DirectApiMethod(Class<T> returnType, String apiMethodName, ArgumentAdapter[] argumentAdapters, TypeAdapter<T> returnTypeAdapter) {
        this.returnType = returnType;
        this.apiMethodName = apiMethodName;
        this.argumentAdapters = argumentAdapters;
        this.returnTypeAdapter = returnTypeAdapter;
    }

    @Override
    public T invoke(ApiComposer apiComposer, Object[] args) throws IOException {
        Request request = convertToRequest(
                apiComposer.endpointProvider().endpoint(),
                apiMethodName,
                argumentAdapters,
                args);
        Response response = null;
        try {
            response = apiComposer.apiClient().newCall(request).execute();
            return convertToReturnType(response);
        } finally {
            closeQuietly(response);
        }
    }

    @SuppressWarnings("unchecked")
    private T convertToReturnType(Response response) throws IOException {
        if (returnTypeAdapter != null) {
            boolean success = false;
            T result;
            try {
                result = returnTypeAdapter.deserialize(response.responseBody().reader());

                // Try to attach the response data to the result object.
                // Not the cleanest way, but no better solution is available
                if (returnType == DataApiResponse.class) {
                    ApiResponse apiResponse = (ApiResponse) result;
                    if (apiResponse.isSuccessful()) {
                        result = (T) new DataApiResponse(
                                apiResponse.resultCode(),
                                apiResponse.message(),
                                response.responseBody().data());
                    }
                }
                success = true;
            } finally {
                if (!success) {
                    closeQuietly(response);
                }
            }

            return result;
        } else {
            return (T) response;
        }
    }
}
