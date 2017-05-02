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

import com.pcloud.PCloudAPIClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import static com.pcloud.networking.ApiMethod.Factory.apiMethodError;

public class ApiComposer {

    public static Builder create() {
        return new Builder();
    }

    private static final List<ApiMethod.Factory> BUILT_IN_FACTORIES = Arrays.asList(
            DirectApiMethod.FACTORY,
            CallWrappedApiMethod.FACTORY,
            MultiCallWrappedApiMethod.FACTORY);

    private EndpointProvider endpointProvider;
    private PCloudAPIClient apiClient;
    private Transformer transformer;
    private List<ResponseInterceptor> interceptors;

    private boolean loadEagerly;
    private Map<Method, ApiMethod<?>> apiMethodsCache = new ConcurrentHashMap<>();
    private List<ApiMethod.Factory> factories = new ArrayList<>(BUILT_IN_FACTORIES);

    private ApiComposer(Builder builder) {
        if (builder.apiClient == null) {
            throw new IllegalArgumentException("PCloudAPIClient instance cannot be null.");
        }

        this.endpointProvider = builder.endpointProvider != null ? builder.endpointProvider : EndpointProvider.DEFAULT;
        this.apiClient = builder.apiClient;
        this.transformer = builder.transformer != null ? builder.transformer : Transformer.create().build();
        this.interceptors = new ArrayList<>(builder.interceptors);
        this.loadEagerly = builder.loadEagerly;
    }

    public Transformer transformer() {
        return transformer;
    }

    public EndpointProvider endpointProvider() {
        return endpointProvider;
    }

    public PCloudAPIClient apiClient() {
        return apiClient;
    }

    public List<ResponseInterceptor> interceptors() {
        return interceptors;
    }

    @SuppressWarnings("unchecked")
    public <T> T compose(Class<T> apiType) {
        validateApiInterface(apiType);

        if (loadEagerly) {
            Method[] methods = apiType.getDeclaredMethods();
            for (Method method : methods) {
                loadApiMethod(method);
            }
        }

        return (T) Proxy.newProxyInstance(apiType.getClassLoader(), new Class<?>[]{apiType},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args)
                            throws Throwable {
                        // If the method is a method from Object then defer to normal invocation.
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }
                        ApiMethod<Object> apiMethod = (ApiMethod<Object>) loadApiMethod(method);
                        return apiMethod.invoke(ApiComposer.this, args);
                    }
                });
    }

    private ApiMethod<?> loadApiMethod(Method javaMethod) {
        ApiMethod<?> apiMethod = apiMethodsCache.get(javaMethod);
        if (apiMethod == null) {
            apiMethod = createApiMethod(javaMethod);
            if (apiMethod == null) {
                throw apiMethodError(javaMethod, "Cannot adapt method, return type '%s' is not supported.",
                        javaMethod.getReturnType().getName());
            }

            apiMethodsCache.put(javaMethod, apiMethod);
        }

        return apiMethod;
    }

    private ApiMethod<?> createApiMethod(Method javaMethod) {
        ApiMethod<?> apiMethod = null;
        Type[] argumentTypes = javaMethod.getGenericParameterTypes();
        Annotation[][] argumentAnnotations = javaMethod.getParameterAnnotations();
        for (ApiMethod.Factory factory : factories) {
            if ((apiMethod = factory.create(this, javaMethod, argumentTypes, argumentAnnotations)) != null) {
                break;
            }
        }
        return apiMethod;
    }

    private static <T> void validateApiInterface(Class<T> apiInterface) {
        if (!apiInterface.isInterface()) {
            throw new IllegalArgumentException("API declaration '" + apiInterface + "' is not an interface.");
        }
        // Avoid a bug in Android's Dalvik VM for versions 4.x, http://b.android.com/58753
        if (apiInterface.getInterfaces().length > 0) {
            throw new IllegalArgumentException("API declarations must not extend other interfaces.");
        }
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static class Builder {

        private EndpointProvider endpointProvider;
        private PCloudAPIClient apiClient;
        private Transformer transformer;
        private List<ResponseInterceptor> interceptors = new LinkedList<>();
        private boolean loadEagerly;

        private Builder() {
        }

        private Builder(ApiComposer composer) {
            this.endpointProvider = composer.endpointProvider;
            this.apiClient = composer.apiClient;
            this.transformer = composer.transformer;
            this.interceptors = new ArrayList<>(composer.interceptors);
        }

        public Builder endpointProvider(EndpointProvider endpointProvider) {
            if (endpointProvider == null) {
                throw new IllegalArgumentException("EndpointProvider argument cannot be null.");
            }
            this.endpointProvider = endpointProvider;
            return this;
        }

        public Builder apiClient(PCloudAPIClient apiClient) {
            if (apiClient == null) {
                throw new IllegalArgumentException("PCloudAPIClient argument cannot be null.");
            }
            this.apiClient = apiClient;
            return this;
        }

        public Builder transformer(Transformer transformer) {
            if (transformer == null) {
                throw new IllegalArgumentException("Transformer argument cannot be null.");
            }
            this.transformer = transformer;
            return this;
        }

        public Builder addInterceptor(ResponseInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("ResponseInterceptor argument cannot be null.");
            }
            this.interceptors.add(interceptor);
            return this;
        }

        public Builder removeInterceptor(ResponseInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("ResponseInterceptor argument cannot be null.");
            }
            this.interceptors.remove(interceptor);
            return this;
        }

        public Builder removeInterceptors(Collection<ResponseInterceptor> interceptors) {
            if (interceptors == null) {
                throw new IllegalArgumentException("ResponseInterceptor collection argument cannot be null.");
            }
            for (ResponseInterceptor r : interceptors) {
                removeInterceptor(r);
            }
            return this;
        }

        public Builder addInterceptors(Collection<ResponseInterceptor> interceptors) {
            if (interceptors == null) {
                throw new IllegalArgumentException("ResponseInterceptor collection argument cannot be null.");
            }
            for (ResponseInterceptor r : interceptors) {
                addInterceptor(r);
            }
            return this;
        }

        public Builder loadEagerly(boolean loadEagerly) {
            this.loadEagerly = loadEagerly;
            return this;
        }

        public ApiComposer create() {
            return new ApiComposer(this);
        }
    }
}
