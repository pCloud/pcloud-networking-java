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

import com.pcloud.networking.client.Endpoint;
import com.pcloud.networking.client.ResponseBody;
import com.pcloud.networking.serialization.TypeAdapter;
import com.pcloud.utils.Types;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

abstract class ApiMethod<T> {

    static final Object[] EMPTY_ARGS = new Object[0];

    abstract T invoke(ApiComposer apiComposer, Object[] args) throws IOException;

    @SuppressWarnings({"WeakerAccess", "unused"})
    abstract static class Factory {

        abstract ApiMethod<?> create(ApiComposer composer,
                                     Method method, Type[] argumentTypes,
                                     Annotation[][] argumentAnnotations);

        protected static String parseMethodNameAnnotation(Method javaMethod) {
            com.pcloud.networking.api.Method methodName =
                    javaMethod.getAnnotation(com.pcloud.networking.api.Method.class);
            if (methodName == null) {
                throw apiMethodError(javaMethod, "Method must be annotated with the @Method annotation");
            }
            if (methodName.value().equals("")) {
                throw apiMethodError(javaMethod, "API method name cannot be empty.");
            }
            return methodName.value();
        }

        protected static boolean isAllowedResponseType(Class<?> type) {
            return ApiResponse.class.isAssignableFrom(type) || type == ResponseBody.class;
        }

        protected static boolean isAllowedResponseType(Type type) {
            return isAllowedResponseType(Types.getRawType(type));
        }

        protected static ResponseAdapter<?> getResponseAdapter(ApiComposer composer, Method method, Type returnType) {
            return getResponseAdapter(composer, method, Types.getRawType(returnType));
        }

        @SuppressWarnings("unchecked")
        protected static <T> ResponseAdapter<T> getResponseAdapter(ApiComposer composer,
                                                                   Method method,
                                                                   Class<T> returnType) {
            if (ApiResponse.class.isAssignableFrom(returnType)) {
                TypeAdapter<T> typeAdapter = getTypeAdapter(composer, method, returnType);
                if (DataApiResponse.class.isAssignableFrom(returnType)) {
                    return new DataApiResponseAdapter<>((TypeAdapter<? extends DataApiResponse>) typeAdapter, returnType, composer.interceptors());
                } else {
                    return new ApiResponseAdapter<>((TypeAdapter<? extends ApiResponse>) typeAdapter, returnType, composer.interceptors());
                }
            } else if (returnType == ResponseBody.class) {
                return (ResponseAdapter<T>) new ResponseBodyAdapter(composer.interceptors());
            } else {
                throw apiMethodError(method, "Return type '%s' is not supported," +
                                " must be or extend from %s, or be '%s'",
                        returnType, Arrays.asList(DataApiResponse.class, ApiResponse.class), ResponseBody.class);
            }
        }

        @SuppressWarnings("rawtypes")
        protected static RequestAdapter getRequestAdapter(ApiComposer composer, Method method,
                                                          Type[] parameterTypes,
                                                          Annotation[][] annotations) {
            int argumentCount = parameterTypes.length;
            boolean hasDataParameter = false;
            boolean hasEndpointParameter = false;

            ArgumentAdapter[] argumentAdapters = new ArgumentAdapter[argumentCount];
            for (int index = 0; index < argumentCount; index++) {
                Annotation[] argumentAnnotations = annotations[index];
                Type parameterType = parameterTypes[index];

                ArgumentAdapter resolvedAdapter = null;
                for (Annotation annotation : argumentAnnotations) {
                    Type annotationType = annotation.annotationType();
                    if (annotationType == RequestBody.class) {
                        TypeAdapter<?> typeAdapter = getTypeAdapter(composer, method, parameterType);
                        resolvedAdapter = ArgumentAdapters.requestBody(typeAdapter);
                    } else if (annotationType == Parameter.class) {
                        String name = ((Parameter) annotation).value();
                        if (name.equals("")) {
                            throw apiMethodError(method, "'@%s'-annotated method arguments cannot have an " +
                                    "empty parameter name.", Parameter.class.getSimpleName());
                        }

                        TypeAdapter<?> typeAdapter = getTypeAdapter(composer, method, parameterType);
                        resolvedAdapter = ArgumentAdapters.parameter(name, typeAdapter);
                    } else if (annotationType == RequestData.class) {
                        if (hasDataParameter) {
                            throw apiMethodError(method, "API method cannot have more than one " +
                                    "parameter annotated with @%s.", RequestData.class.getSimpleName());
                        }

                        hasDataParameter = true;
                        resolvedAdapter = ArgumentAdapters.dataSource(parameterType);
                    }
                }

                if (resolvedAdapter == null && parameterType == Endpoint.class) {
                    if (hasEndpointParameter) {
                        throw apiMethodError(method, "API method cannot have more than one " +
                                "non-annotated '%s' parameter ", Endpoint.class);
                    }
                    hasEndpointParameter = true;
                    resolvedAdapter = ArgumentAdapters.endpoint();
                }

                if (resolvedAdapter == null) {
                    throw apiMethodError(method, "Each API method parameter must be annotated with one of %s " +
                                    "or be of type '%s'.",
                            Arrays.<Type>asList(RequestBody.class, Parameter.class, RequestData.class), Endpoint.class);
                }

                argumentAdapters[index] = resolvedAdapter;
            }

            return new DefaultRequestAdapter(argumentAdapters);
        }

        protected static <T> TypeAdapter<T> getTypeAdapter(ApiComposer composer, Method method, Type parameterType) {
            TypeAdapter<T> typeAdapter = composer.transformer().getTypeAdapter(parameterType);
            if (typeAdapter == null) {
                throw apiMethodError(method,
                        "Cannot find a suitable TypeAdapter for argument of type '%s'.",
                        parameterType
                );
            }
            return typeAdapter;
        }

        protected static void checkMethodThrowsExceptions(Method method, Type... exceptionTypes) {
            List<Class<?>> declaredExceptions = Arrays.asList(method.getExceptionTypes());
            List<Type> expectedExceptions = Arrays.asList(exceptionTypes);
            //noinspection SlowListContainsAll,SuspiciousMethodCalls
            if (!declaredExceptions.containsAll(expectedExceptions)) {
                throw apiMethodError(method, "Method should declare that it throws %s.", expectedExceptions);
            }
        }

        @SuppressWarnings("SameParameterValue")
        protected static RuntimeException apiMethodError(Method method,
                                                         Throwable cause,
                                                         String message,
                                                         Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message +
                    "\n for method " +
                    method.getDeclaringClass().getSimpleName() +
                    "." +
                    method.getName(), cause);
        }

        protected static RuntimeException apiMethodError(Method method, String message, Object... args) {
            return apiMethodError(method, null, message, args);
        }
    }
}


