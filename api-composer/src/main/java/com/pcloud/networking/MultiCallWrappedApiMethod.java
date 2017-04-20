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
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.pcloud.networking.Types.getRawType;

class MultiCallWrappedApiMethod<T, R> extends ApiMethod<MultiCall<T, R>> {

    static final ApiMethod.Factory FACTORY = new Factory();

    private interface RequestContainerAdapter<T, R> {
        List<R> convert(T requestsContainer);
    }

    private String apiMethodName;

    private RequestContainerAdapter<Object, T> argumentsRequestContainerAdapter;
    private RequestAdapter requestAdapter;
    private ResponseAdapter<R> returnTypeAdapter;

    private MultiCallWrappedApiMethod(String apiMethodName, RequestContainerAdapter<Object, T> argumentsRequestContainerAdapter, RequestAdapter requestAdapter, ResponseAdapter<R> returnTypeAdapter) {
        this.apiMethodName = apiMethodName;
        this.argumentsRequestContainerAdapter = argumentsRequestContainerAdapter;
        this.requestAdapter = requestAdapter;
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
            requestAdapter.adapt(builder, request);
            rawRequests.add(builder.build());
        }

        com.pcloud.MultiCall rawCall = apiComposer.apiClient()
                .newCall(rawRequests);
        return new ApiClientMultiCall<>(rawCall, returnTypeAdapter, requests);
    }

    private static class Factory extends ApiMethod.Factory {

        @Override
        public ApiMethod<?> create(ApiComposer composer, Method method, Type[] argumentTypes, Annotation[][] argumentAnnotations) {
            Type returnType = method.getGenericReturnType();
            Type rawType = getRawType(method.getReturnType());
            if (rawType != MultiCall.class) {
                return null;
            }

            if (!(returnType instanceof ParameterizedType)) {
                throw new IllegalStateException("Call return type must be parameterized"
                        + " as MultiCall<Foo, Bar> or MultiCall<Foo,? extends Bar>");
            }

            final Class<?> innerReturnType = getRawType(
                    Types.getParameterUpperBound(1, (ParameterizedType) returnType));
            if (!isAllowedResponseType(innerReturnType)) {
                return null;
            }

            if (DataApiResponse.class.isAssignableFrom(innerReturnType)) {
                throw apiMethodError(method, "Return types assignable from '%s' are " +
                        "not supported for MultiCall<>- returning API methods.", DataApiResponse.class);
            }

            final Class<?> expectedRequestType = getRawType(Types.getParameterUpperBound(0, (ParameterizedType) returnType));

            String apiMethodName = parseMethodNameAnnotation(method);
            checkArgumentAnnotations(method, argumentAnnotations);

            @SuppressWarnings("unchecked")
            RequestContainerAdapter<Object, ?> containerAdapter =
                    (RequestContainerAdapter<Object, ?>) getContainerAdapter(method, argumentTypes[0], expectedRequestType);
            RequestAdapter requestAdapter = createRequestAdapter(composer, method, new Type[]{expectedRequestType}, argumentAnnotations);
            ResponseAdapter<?> returnTypeAdapter = getResponseAdapter(composer, method, innerReturnType);

            return new MultiCallWrappedApiMethod<>(apiMethodName, containerAdapter, requestAdapter, returnTypeAdapter);
        }

        private static <T> RequestContainerAdapter<?, T> getContainerAdapter(Method method, Type requestContainerType, Class<?> expectedRequestType) {
            Type actualRequestType;
            RequestContainerAdapter<?, T> containerAdapter;
            Type elementType = Types.arrayComponentType(requestContainerType);
            if (elementType != null) {
                // Requests are given as an array or var-args.
                actualRequestType = elementType;
                containerAdapter = new RequestContainerAdapter<T[], T>() {
                    @Override
                    public List<T> convert(T[] requestsContainer) {
                        return Arrays.asList(requestsContainer);
                    }
                };
            } else if (List.class.isAssignableFrom(getRawType(requestContainerType))) {
                // Requests are given as a List/
                if (!(requestContainerType instanceof ParameterizedType)) {
                    throw new IllegalStateException("Requests List type must be parameterized"
                            + " as List<Foo> or List<? extends Foo>");
                }
                actualRequestType = Types.collectionElementType(requestContainerType, Collection.class);
                containerAdapter = new RequestContainerAdapter<List<T>, T>() {
                    @Override
                    public List<T> convert(List<T> requestsContainer) {
                        return requestsContainer;
                    }
                };

            } else {
                throw apiMethodError(method, "MultiCall-returning methods should have a single, @RequestBody-annotated argument " +
                        ", either an array or a java.util.List<> of the Request type.");
            }

            if (!Types.equals(expectedRequestType, actualRequestType)) {
                throw apiMethodError(method, "Method returns MultiCall<%s,%s>, but expects " +
                        "the request arguments to be of type %s." +
                        " The first generic type and the method " +
                        "argument type must match.", expectedRequestType, method.getGenericReturnType(), actualRequestType);
            }

            return containerAdapter;
        }

        private static void checkArgumentAnnotations(Method method, Annotation[][] annotations) {
            if (annotations.length == 0) {
                throw apiMethodError(method, "No request argument provided.");
            }
            if (annotations.length > 1) {
                throw apiMethodError(method, "More than one request argument provided.");
            }

            Annotation[] containerAnnotations = annotations[0];
            RequestBody bodyAnnotation = null;
            for (Annotation annotation : containerAnnotations) {
                Type annotationType = annotation.annotationType();
                if (annotationType == com.pcloud.networking.RequestBody.class) {
                    bodyAnnotation = (RequestBody) annotation;
                } else if (annotationType == Parameter.class) {
                    throw apiMethodError(method, "MultiCall methods cannot have @Parameter-annotated arguments.");
                } else if (annotationType == RequestData.class) {
                    throw apiMethodError(method, "MultiCall methods cannot have @RequestData-annotated arguments.");
                }
            }

            if (bodyAnnotation == null) {
                throw apiMethodError(method, "MultiCall methods must have a single, @RequestBody-annotated argument.");
            }
        }
    }
}
