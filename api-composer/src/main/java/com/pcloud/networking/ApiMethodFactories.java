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

import com.pcloud.Endpoint;
import com.pcloud.Request;
import com.pcloud.Response;
import com.pcloud.ResponseBody;
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.pcloud.IOUtils.closeQuietly;

public class ApiMethodFactories {

    private ApiMethodFactories(){

    }

    class WrappedApiMethod<T> implements ApiMethod<Call<T>> {

        @Override
        public Call<T> invoke(ApiComposer apiComposer, Object[] args) throws IOException {

        }
    }

    class SingleResponseApiMethod<T> implements ApiMethod<T> {

        private ApiComposer apiComposer;
        private String apiMethodName;
        private ArgumentAdapter[] argumentAdapters;
        private TypeAdapter<T> returnTypeAdapter;
        private Class<T> returnType;

        private SingleResponseApiMethod(ApiComposer apiComposer, Class<T> returnType, String apiMethodName, ArgumentAdapter[] argumentAdapters, TypeAdapter<T> returnTypeAdapter) {
            this.apiComposer = apiComposer;
            this.returnType = returnType;
            this.apiMethodName = apiMethodName;
            this.argumentAdapters = argumentAdapters;
            this.returnTypeAdapter = returnTypeAdapter;
        }

        T invoke(Object[] args) throws IOException {
            Request request = convertToRequest(args);
            Endpoint endpoint = apiComposer.endpointProvider().endpoint(request);
            if (!request.endpoint().equals(endpoint)) {
                request.newRequest()
                        .endpoint(endpoint)
                        .build();
            }

            Response response = apiComposer.apiClient().newCall(request).execute();
            return convertToReturnType(response);
        }

        com.pcloud.Request convertToRequest(final ArgumentAdapter<? extends Object>[] argumentAdapters, final Object[] methodArgs) {
            final Request.Builder requestBuilder = Request.create()
                    .methodName(apiMethodName);

            requestBuilder.body(new com.pcloud.RequestBody() {
                @SuppressWarnings("unchecked")
                @Override
                public void writeТо(ProtocolWriter writer) throws IOException {
                    for (int index = 0; index < methodArgs.length; index++) {
                        argumentAdapters[index].adapt(requestBuilder, writer, methodArgs[index]);
                    }
                }
            });
            return requestBuilder.build();
        }

        @SuppressWarnings("unchecked")
        T convertToReturnType(Response response) throws IOException {
            if (returnTypeAdapter != null){
                boolean success = false;
                T result;
                try {
                    result = returnTypeAdapter.deserialize(response.responseBody().reader());

                    // Try to attach the response data to the result object.
                    // Not the cleanest way, but no better solution is available
                    if (returnType == DataApiResponse.class){
                        ApiResponse apiResponse = (ApiResponse) result;
                        if (apiResponse.isSuccessful()){
                            result = (T) new DataApiResponse(
                                    apiResponse.resultCode(),
                                    apiResponse.message(),
                                    response.responseBody().data());
                        }
                    }
                    success = true;
                } finally {
                    if (!success){
                        closeQuietly(response);
                    }
                }

                return result;
            } else {
                return (T) response;
            }
        }


    }

    <T> ApiMethod.Factory<T> directMethodFactory(){
        return new ApiMethod.Factory<T>() {
            @Override
            public ApiMethod<T> create(ApiComposer composer, java.lang.reflect.Method method) {
                return new ApiMethod.Builder<T>(composer, method).create();
            }
        };
    }

    class DirectMethodfactory<T> implements ApiMethod.Factory<T> {
        @Override
        public ApiMethod<T> create(ApiComposer composer, java.lang.reflect.Method method) {
            return new ApiMethod.Builder<T>(composer, method).create();
        }
    }

    class CallWrappedFactory<T> implements ApiMethod.Factory<Call<T>> {

        @Override
        public ApiMethod<Call<T>> create(ApiComposer composer, java.lang.reflect.Method method) {
            return null;
        }
    }

    class Builder<T> {

        private String apiMethodName;
        private ArgumentAdapter<?>[] argumentAdapters;
        private TypeAdapter<T> returnTypeAdapter;

        private boolean hasDataParameter;
        private ApiComposer apiComposer;
        private java.lang.reflect.Method javaMethod;
        private Class<T> returnType;

        Builder(ApiComposer composer, java.lang.reflect.Method javaMethod) {
            this.apiComposer = composer;
            this.javaMethod = javaMethod;

            returnType = (Class<T>) javaMethod.getReturnType();
            if (ApiResponse.class.isAssignableFrom(returnType) ){
                returnTypeAdapter = apiComposer.transformer().getTypeAdapter(returnType);
            } else if (returnType == ResponseBody.class) {
                returnTypeAdapter = null;
            } else {
                // Short-circuit to allow composition.
                returnType = null;
                return;
            }

            com.pcloud.networking.Method methodName = javaMethod.getAnnotation(com.pcloud.networking.Method.class);
            if (methodName == null) {
                throw apiMethodError("Method must be annotated with the @Method annotation");
            }
            if (methodName.value().equals("")) {
                throw apiMethodError("API method name cannot be empty.");
            }

            apiMethodName = methodName.value();

            Type[] parameterTypes = javaMethod.getParameterTypes();
            Annotation[][] parameterAnnotations = javaMethod.getParameterAnnotations();
            argumentAdapters = new ArgumentAdapter[parameterTypes.length];
            for (int index = 0; index < parameterTypes.length; index++) {
                argumentAdapters[index] = resolveArgumentAdapter(parameterTypes[index], parameterAnnotations[index]);
            }
        }


        com.pcloud.networking.ApiMethod<T> create() {
            if (returnType == null) {
                return null;
            }
            return new ApiMethod.SingleResponseApiMethod<>(apiComposer, returnType, apiMethodName, argumentAdapters, returnTypeAdapter);
        }


        private ArgumentAdapter<?> resolveArgumentAdapter(Type parameterType, Annotation[] annotations) {
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
                        throw apiMethodError("@Parameter-annotated methods cannot have an " +
                                "empty parameter name.");
                    }
                    TypeAdapter<?> typeAdapter = transformer.getTypeAdapter(parameterType);
                    //TODO: Add check if type adapter cannot be resolved
                    return ArgumentAdapters.parameter(name, typeAdapter, parameterType);
                } else if (annotationType == RequestData.class) {
                    if (hasDataParameter) {
                        throw apiMethodError("API method cannot have more than one " +
                                "parameter annotated with @RequestData");
                    }

                    hasDataParameter = true;
                    return ArgumentAdapters.dataSource();
                }
            }

            throw apiMethodError("Cannot adapt method parameter, missing @Parameter, @RequestBody or @RequestData annotation.");
        }

        private RuntimeException apiMethodError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + javaMethod.getDeclaringClass().getSimpleName()
                    + "."
                    + javaMethod.getName(), cause);
        }

        private RuntimeException apiMethodError(String message, Object... args) {
            return apiMethodError(null, message, args);
        }
    }



}
