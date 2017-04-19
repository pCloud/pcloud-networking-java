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

import com.pcloud.*;
import com.pcloud.RequestBody;
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static com.pcloud.IOUtils.closeQuietly;

abstract class ApiMethod<T> {

    public static final Factory FAILING_FACTORY = new Factory() {
        @Override
        ApiMethod<?> create(ApiComposer composer, Method method, Class<?>[] argumentTypes, Annotation[][] argumentAnnotations) {
            throw apiMethodError(method, "Cannot adapt method, return type '%s' is not supported.", method.getReturnType().getName());
        }
    };

    abstract T invoke(ApiComposer apiComposer, Object[] args) throws IOException;

    abstract static class Factory {

        abstract com.pcloud.networking.ApiMethod<?> create(ApiComposer composer, Method method, Class<?>[] argumentTypes, Annotation[][] argumentAnnotations);

        protected static RuntimeException apiMethodError(Method method, Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + method.getDeclaringClass().getSimpleName()
                    + "."
                    + method.getName(), cause);
        }

        protected static String parseMethodNameAnnotation(Method javaMethod) {
            com.pcloud.networking.Method methodName = javaMethod.getAnnotation(com.pcloud.networking.Method.class);
            if (methodName == null) {
                throw apiMethodError(javaMethod, "Method must be annotated with the @Method annotation");
            }
            if (methodName.value().equals("")) {
                throw apiMethodError(javaMethod, "API method name cannot be empty.");
            }
            return methodName.value();
        }

        protected static RuntimeException apiMethodError(Method method, String message, Object... args) {
            return apiMethodError(method, null, message, args);
        }

    }

    protected static com.pcloud.Request convertToRequest(Endpoint endpoint, String apiMethodName, final ArgumentAdapter[] argumentAdapters, final Object[] methodArgs) {
        final Request.Builder requestBuilder = Request.create()
                .methodName(apiMethodName)
                .endpoint(endpoint);

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
}


