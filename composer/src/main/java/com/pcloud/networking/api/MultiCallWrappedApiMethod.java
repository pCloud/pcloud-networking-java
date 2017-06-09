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

import com.pcloud.networking.client.Request;
import com.pcloud.networking.client.MultiCall;
import com.pcloud.utils.Types;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pcloud.utils.Types.getParameterUpperBound;
import static com.pcloud.utils.Types.getRawType;

class MultiCallWrappedApiMethod<T, R> extends ApiMethod<R> {

    static final ApiMethod.Factory FACTORY = new Factory();

    private interface RequestContainerAdapter<T, R> {
        Type requestType();

        List<R> convert(T requestsContainer);
    }

    private String apiMethodName;

    private RequestContainerAdapter<Object, T> argumentsRequestContainerAdapter;
    private RequestAdapter requestAdapter;
    private ResponseAdapter<R> returnTypeAdapter;
    private CallAdapter callAdapter;

    private MultiCallWrappedApiMethod(String apiMethodName,
                                      RequestContainerAdapter<Object, T> argumentsRequestContainerAdapter,
                                      RequestAdapter requestAdapter,
                                      ResponseAdapter<R> returnTypeAdapter,
                                      CallAdapter callAdapter) {
        this.apiMethodName = apiMethodName;
        this.argumentsRequestContainerAdapter = argumentsRequestContainerAdapter;
        this.requestAdapter = requestAdapter;
        this.returnTypeAdapter = returnTypeAdapter;
        this.callAdapter = callAdapter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R invoke(ApiComposer apiComposer, Object[] args) throws IOException {
        List<T> requests = argumentsRequestContainerAdapter.convert(args[0]);
        if (requests == null) {
            throw new IllegalArgumentException("The requests container cannot be null!");
        }
        List<Request> rawRequests = new ArrayList<>(requests.size());
        final Request.Builder builder = Request.create()
                .endpoint(apiComposer.endpointProvider().endpoint())
                .methodName(apiMethodName);

        for (final T request : requests) {
            requestAdapter.adapt(builder, request);
            rawRequests.add(builder.build());
        }

        MultiCall rawCall = apiComposer.apiClient()
                .newCall(rawRequests);
        return (R) callAdapter.adapt(new ApiClientMultiCall<>(apiComposer, rawCall, returnTypeAdapter, requests));
    }

    private static class Factory extends ApiMethod.Factory {

        @Override
        public ApiMethod<?> create(ApiComposer composer, Method method,
                                   Type[] argumentTypes, Annotation[][] argumentAnnotations) {
            if (argumentTypes.length == 0) {
                return null;
            }

            Type requestContainerType = argumentTypes[0];
            String apiMethodName = parseMethodNameAnnotation(method);

            if (!hasRequestBodyAnnotation(argumentAnnotations) ||
                    !hasValidRequestContainerType(requestContainerType)) {
                return null;
            }

            checkArgumentAnnotations(method, argumentAnnotations);

            @SuppressWarnings("unchecked")
            RequestContainerAdapter<Object, ?> containerAdapter =
                    (RequestContainerAdapter<Object, ?>) getContainerAdapter(method, requestContainerType);

            RequestAdapter requestAdapter = getRequestAdapter(composer, method,
                    new Type[]{containerAdapter.requestType()}, argumentAnnotations);

            CallAdapter<?, ?> callAdapter = composer.nextCallAdapter(method);

            final Class<?> innerReturnType = getRawType(callAdapter.responseType());

            if (!isAllowedResponseType(innerReturnType)) {
                return null;
            }

            if (DataApiResponse.class.isAssignableFrom(innerReturnType)) {
                throw apiMethodError(method, "Return types assignable from '%s' are " +
                        "not supported for MultiCall<>- returning API methods.", DataApiResponse.class);
            }

            ResponseAdapter<?> returnTypeAdapter = getResponseAdapter(composer, method, innerReturnType);

            return new MultiCallWrappedApiMethod<>(apiMethodName, containerAdapter,
                    requestAdapter, returnTypeAdapter, callAdapter);
        }

        private static boolean hasValidRequestContainerType(Type requestContainerType) {
            return Types.arrayComponentType(requestContainerType) != null ||
                    List.class.isAssignableFrom(getRawType(requestContainerType));
        }

        private static <T> RequestContainerAdapter<?, T> getContainerAdapter(Method method, Type requestContainerType) {
            final Type actualRequestType;
            RequestContainerAdapter<?, T> containerAdapter;
            Type elementType = Types.arrayComponentType(requestContainerType);
            if (elementType != null) {
                // Requests are given as an array or var-args.
                actualRequestType = elementType;
                containerAdapter = new RequestContainerAdapter<T[], T>() {
                    @Override
                    public Type requestType() {
                        return actualRequestType;
                    }

                    @Override
                    public List<T> convert(T[] requestsContainer) {
                        if (requestsContainer == null) {
                            throw new IllegalArgumentException("The requests container cannot be null!");
                        }
                        return Arrays.asList(requestsContainer);
                    }
                };
            } else if (List.class.isAssignableFrom(getRawType(requestContainerType))) {
                // Requests are given as a List/
                if (!(requestContainerType instanceof ParameterizedType)) {
                    throw new IllegalStateException("Requests List type must be parameterized" +
                            " as List<Foo> or List<? extends Foo>");
                }
                actualRequestType = getParameterUpperBound(0, requestContainerType);
                containerAdapter = new RequestContainerAdapter<List<T>, T>() {
                    @Override
                    public Type requestType() {
                        return actualRequestType;
                    }

                    @Override
                    public List<T> convert(List<T> requestsContainer) {
                        return requestsContainer;
                    }
                };

            } else {
                throw apiMethodError(method,
                        "MultiCall-returning methods should have a single, @RequestBody-annotated argument " +
                        ", either an array or a java.util.List<> of the Request type.");
            }

            return containerAdapter;
        }

        private static boolean hasRequestBodyAnnotation(Annotation[][] annotations) {
            if (annotations.length == 1) {
                Annotation[] containerAnnotations = annotations[0];
                for (Annotation annotation : containerAnnotations) {
                    Type annotationType = annotation.annotationType();
                    if (annotationType == RequestBody.class) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static void checkArgumentAnnotations(Method method, Annotation[][] annotations) {

            Annotation[] containerAnnotations = annotations[0];
            for (Annotation annotation : containerAnnotations) {
                Type annotationType = annotation.annotationType();
                if (annotationType == Parameter.class) {
                    throw apiMethodError(method, "MultiCall methods cannot have @Parameter-annotated arguments.");
                } else if (annotationType == RequestData.class) {
                    throw apiMethodError(method, "MultiCall methods cannot have @RequestData-annotated arguments.");
                }
            }
        }
    }
}
