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
import com.pcloud.ResponseBody;
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.pcloud.networking.Types.getRawType;

class MultiCallWrappedApiMethod<T, R> extends ApiMethod<MultiCall<T, R>> {

    static final ApiMethod.Factory FACTORY = new Factory();

    private interface RequestContainerAdapter<T,R> {
        List<R> convert(T requestsContainer);
    }

    private static class Factory extends ApiMethod.Factory {

        @Override
        public ApiMethod<?> create(ApiComposer composer, Method method, Class<?>[] argumentTypes, Annotation[][] argumentAnnotations) {
            Type returnType = method.getGenericReturnType();
            Type rawType = getRawType(method.getReturnType());
            if (rawType != MultiCall.class) {
                return null;
            }

            if (!(returnType instanceof ParameterizedType)) {
                throw new IllegalStateException("Call return type must be parameterized"
                        + " as MultiCall<Foo, Bar> or MultiCall<Foo,? extends Bar>");
            }

            Class<?> requestType = getRawType(Types.getParameterUpperBound(0, (ParameterizedType) returnType));

            Class<?> innerReturnType = getRawType(Types.getParameterUpperBound(1, (ParameterizedType) returnType));
            if (!ApiResponse.class.isAssignableFrom(innerReturnType) &&
                    innerReturnType != ResponseBody.class) {
                return null;
            }

            return new Builder<>(composer, method, argumentAnnotations, innerReturnType, requestType).create();
        }

        static class Builder<T, R> {

            private String apiMethodName;
            private TypeAdapter<R> returnTypeAdapter;
            private TypeAdapter<T> requestTypeAdapter;
            private RequestContainerAdapter<Object, T> requestsAdapter;

            Builder(ApiComposer composer, Method method, Annotation[][] argumentAnnotations, Type returnType, Class<?> requestType) {
                if (ApiResponse.class.isAssignableFrom(getRawType(returnType))) {
                    returnTypeAdapter = composer.transformer().getTypeAdapter(returnType);
                } else {
                    returnTypeAdapter = null;
                }

                apiMethodName = parseMethodNameAnnotation(method);
                Type[] parameterTypes = method.getGenericParameterTypes();
                checkParameterCount(method, parameterTypes);

                Type requestContainerType = parameterTypes[0];

                checkArgumentAnnotations(method, argumentAnnotations[0]);

                Type actualRequestType = resolveRequestType(composer, method, requestContainerType);

                if (!Types.equals(requestType, actualRequestType)) {
                    throw apiMethodError(method, "Method returns MultiCall<%s,%s>, but expects the request arguments to be of type %s." +
                            " The first generic type and the method argument type must match.", requestType, returnType, actualRequestType);
                }

                requestTypeAdapter = composer.transformer().getTypeAdapter(actualRequestType);
                returnTypeAdapter = composer.transformer().getTypeAdapter(returnType);
            }

            com.pcloud.networking.ApiMethod<MultiCall<T,R>> create() {
                return new MultiCallWrappedApiMethod<>(apiMethodName, requestsAdapter,requestTypeAdapter, returnTypeAdapter);
            }

            private void checkRequestTypesMatch(Class<?> expectedArgumentType){

            }

            private Type resolveRequestType(ApiComposer composer, Method method, Type requestContainerType) {
                Type elementType = Types.arrayComponentType(requestContainerType);
                Type actualRequestType;
                if (elementType != null) {
                    // Requests are given as an array or var-args.
                    requestsAdapter = new RequestContainerAdapter<Object, T>() {
                        @Override
                        public List<T> convert(Object requestsContainer) {
                            return Arrays.asList((T[])requestsContainer);
                        }
                    };
                    actualRequestType = elementType;
                } else if (getRawType(requestContainerType).isAssignableFrom(List.class)) {
                    if (!(requestContainerType instanceof ParameterizedType)) {
                        throw new IllegalStateException("Requests List type must be parameterized"
                                + " as List<Foo> or List<? extends Foo>");
                    }
                    requestsAdapter = new RequestContainerAdapter<Object, T>() {
                        @Override
                        public List<T> convert(Object requestsContainer) {
                            return (List<T>) requestsContainer;
                        }
                    };

                    actualRequestType = Types.collectionElementType(requestContainerType, Collection.class);
                } else {
                    throw apiMethodError(method, "MultiCall-returning methods should have a single, @RequestBody-annotated argument " +
                            ", either an array or a java.util.List<> of the Request type.");
                }

                return actualRequestType;
            }

            private void checkParameterCount(Method method, Type[] parameterTypes) {
                if (parameterTypes.length == 0){
                    throw apiMethodError(method, "No request argument provided.");
                }
                if (parameterTypes.length > 1){
                    throw apiMethodError(method, "More than one request argument provided.");
                }
            }

            private void checkArgumentAnnotations(Method method, Annotation[] annotations) {
                RequestBody bodyAnnotation = null;
                for (Annotation annotation : annotations) {
                    Type annotationType = annotation.annotationType();
                    if (annotationType == com.pcloud.networking.RequestBody.class) {
                        bodyAnnotation = (RequestBody) annotation;
                    } else if (annotationType == Parameter.class) {
                        throw apiMethodError(method, "MultiCall methods cannot have @Parameter-annotated arguments.");
                    } else if (annotationType == RequestData.class) {
                        throw apiMethodError(method, "MultiCall methods cannot have @RequestData-annotated arguments.");
                    }
                }

                if(bodyAnnotation == null) {
                    throw apiMethodError(method, "MultiCall methods must have a single, @RequestBody-annotated argument.");
                }
            }
        }
    }

    private String apiMethodName;
    private RequestContainerAdapter<Object, T> argumentsRequestContainerAdapter;
    private TypeAdapter<T> requestTypeAdapter;
    private TypeAdapter<R> returnTypeAdapter;

    private MultiCallWrappedApiMethod(String apiMethodName, RequestContainerAdapter<Object, T> argumentsRequestContainerAdapter, TypeAdapter<T> requestTypeAdapter, TypeAdapter<R> returnTypeAdapter) {
        this.apiMethodName = apiMethodName;
        this.argumentsRequestContainerAdapter = argumentsRequestContainerAdapter;
        this.requestTypeAdapter = requestTypeAdapter;
        this.returnTypeAdapter = returnTypeAdapter;
    }

    @Override
    public MultiCall<T, R> invoke(ApiComposer apiComposer, Object[] args) throws IOException {
        List<T> requests = argumentsRequestContainerAdapter.convert(args[0]);
        List<Request> rawRequests = new ArrayList<>(requests.size());
        final Request.Builder builder = Request.create()
                .endpoint(apiComposer.endpointProvider().endpoint())
                .methodName(apiMethodName);

        for (final T request : requests) {
            builder.body(new com.pcloud.RequestBody() {
                @Override
                public void writeТо(ProtocolWriter writer) throws IOException {
                    requestTypeAdapter.serialize(writer, request);
                }
            });
            rawRequests.add(builder.build());
        }

        com.pcloud.MultiCall rawCall = apiComposer.apiClient()
                .newCall(rawRequests);
        return new ApiClientMultiCall<>(rawCall, returnTypeAdapter, requests);
    }

}
