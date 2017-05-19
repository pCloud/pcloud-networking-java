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
import com.pcloud.PCloudAPIClient;

import java.io.IOException;
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

/**
 * Composes implementations for interfaces in which you have provided methods that describe your network calls
 *
 * @see EndpointProvider
 * @see PCloudAPIClient
 * @see ResponseInterceptor
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ApiComposer {
    /**
     * Creates and returns a {@linkplain Builder} to build the {@linkplain ApiComposer} with
     *
     * @return A new instance of a {@linkplain Builder} to build the {@linkplain ApiComposer} with
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Internal list of {@linkplain ApiMethod.Factory} factories
     * <p>
     * Holds the list of available {@linkplain ApiMethod.Factory} implementations.
     * <p>
     * <b>Note that the order of elements is important, please consider it before modification.</b>
     */
    private static final List<ApiMethod.Factory> BUILT_IN_API_METHOD_FACTORIES = Arrays.asList(
            MultiCallWrappedApiMethod.FACTORY,
            CallWrappedApiMethod.FACTORY,
            DirectApiMethod.FACTORY
    );

    /**
     * The built-in set of {@linkplain CallAdapter.Factory} implementations
     */
    private static final List<CallAdapter.Factory> BUILD_IN_CALL_ADAPTER_FACTORIES = Arrays.asList(
            CallWrappedCallAdapter.FACTORY,
            DirectCallAdapter.FACTORY
    );

    private EndpointProvider endpointProvider;
    private PCloudAPIClient apiClient;
    private Transformer transformer;
    private List<ResponseInterceptor> interceptors;

    private boolean loadEagerly;
    private Map<Method, ApiMethod<?>> apiMethodsCache = new ConcurrentHashMap<>();
    private List<ApiMethod.Factory> factories = new ArrayList<>(BUILT_IN_API_METHOD_FACTORIES);
    private List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(BUILD_IN_CALL_ADAPTER_FACTORIES);

    private ApiComposer(Builder builder) {
        if (builder.apiClient == null) {
            throw new IllegalArgumentException("PCloudAPIClient instance cannot be null.");
        }

        this.endpointProvider = builder.endpointProvider != null ? builder.endpointProvider : EndpointProvider.DEFAULT;
        this.apiClient = builder.apiClient;
        this.transformer = builder.transformer != null ? builder.transformer : Transformer.create().build();
        this.interceptors = new ArrayList<>(builder.interceptors);
        this.callAdapterFactories.addAll(builder.callAdapterFactories);
        this.loadEagerly = builder.loadEagerly;
    }

    /**
     * Returns the {@linkplain Transformer} you provided in the {@linkplain Builder}
     *
     * @return The {@linkplain Transformer} you provided in the {@linkplain Builder}
     */
    public Transformer transformer() {
        return transformer;
    }

    /**
     * Returns the {@linkplain EndpointProvider} you provided in the {@linkplain Builder}
     *
     * @return The {@linkplain EndpointProvider} you provided in the {@linkplain Builder}
     */
    public EndpointProvider endpointProvider() {
        return endpointProvider;
    }

    /**
     * Returns the {@linkplain PCloudAPIClient} you provided in the {@linkplain Builder}
     *
     * @return The {@linkplain PCloudAPIClient} you provided in the {@linkplain Builder}
     */
    public PCloudAPIClient apiClient() {
        return apiClient;
    }

    /**
     * Returns a {@linkplain List} of all the {@linkplain ResponseInterceptor}
     * objects you provided in the {@linkplain Builder}
     *
     * @return A {@linkplain List} of all the {@linkplain ResponseInterceptor}
     * objects you provided in the {@linkplain Builder}
     */
    public List<ResponseInterceptor> interceptors() {
        return interceptors;
    }

    /**
     * Composes an instance of the java interface which houses the network call methods
     * <p>
     * Note that you should provide an interface which does not extend anything or else this wont work!
     *
     * @param apiType The class of the java interface in which you have
     *                written the methods to represent the network calls
     * @param <T>     The generic type of the returned instance
     * @return An instance of a class of the same generic type as the class you provided as an argument
     * implementing the interface in which you have written methods to represent your network calls
     * @throws RuntimeException if {@linkplain #loadEagerly} is set to true and if the instantiation fails
     */
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

    CallAdapter<?, ?> nextCallAdapter(Method method) {
        CallAdapter<?, ?> callAdapter = null;
        for (CallAdapter.Factory adapterFactory : callAdapterFactories) {
            if ((callAdapter = adapterFactory.get(this, method)) != null) {
                break;
            }
        }
        return callAdapter;
    }

    void notifyIOError(Endpoint endpoint, IOException error){
        endpointProvider.endpointConnectionError(endpoint, error);
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

    /**
     * Returns a new instance of a {@linkplain Builder} to build a new {@linkplain ApiComposer}
     *
     * @return A new instance of a {@linkplain Builder} to build a new {@linkplain ApiComposer}
     */
    public Builder newBuilder() {
        this.callAdapterFactories.removeAll(BUILD_IN_CALL_ADAPTER_FACTORIES);
        return new Builder(this);
    }

    /**
     * A builder to build instances of {@linkplain ApiComposer}
     */
    public static class Builder {

        private EndpointProvider endpointProvider;
        private PCloudAPIClient apiClient;
        private Transformer transformer;
        private List<ResponseInterceptor> interceptors = new LinkedList<>();
        private List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
        private boolean loadEagerly;

        private Builder() {
        }

        private Builder(ApiComposer composer) {
            this.endpointProvider = composer.endpointProvider;
            this.apiClient = composer.apiClient;
            this.transformer = composer.transformer;
            this.interceptors = new ArrayList<>(composer.interceptors);
            this.callAdapterFactories = new ArrayList<>(composer.callAdapterFactories);
        }

        /**
         * Sets the {@linkplain EndpointProvider} for the {@linkplain ApiComposer}
         *
         * @param endpointProvider The {@linkplain EndpointProvider} to be set to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain EndpointProvider} argument
         */
        public Builder endpointProvider(EndpointProvider endpointProvider) {
            if (endpointProvider == null) {
                throw new IllegalArgumentException("EndpointProvider argument cannot be null.");
            }
            this.endpointProvider = endpointProvider;
            return this;
        }

        /**
         * Sets the {@linkplain PCloudAPIClient} for the {@linkplain ApiComposer}
         *
         * @param apiClient The {@linkplain PCloudAPIClient} to be set to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain PCloudAPIClient} argument
         */
        public Builder apiClient(PCloudAPIClient apiClient) {
            if (apiClient == null) {
                throw new IllegalArgumentException("PCloudAPIClient argument cannot be null.");
            }
            this.apiClient = apiClient;
            return this;
        }

        /**
         * Sets the {@linkplain Transformer} for this {@linkplain ApiComposer}
         *
         * @param transformer The {@linkplain Transformer} to be set to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain Transformer} argument
         */
        public Builder transformer(Transformer transformer) {
            if (transformer == null) {
                throw new IllegalArgumentException("Transformer argument cannot be null.");
            }
            this.transformer = transformer;
            return this;
        }

        /**
         * Sets a single {@linkplain ResponseInterceptor} for this {@linkplain ApiComposer}
         *
         * @param interceptor The {@linkplain ResponseInterceptor} to be set to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain ResponseInterceptor} argument
         */
        public Builder addInterceptor(ResponseInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("ResponseInterceptor argument cannot be null.");
            }
            this.interceptors.add(interceptor);
            return this;
        }

        /**
         * Removes a single {@linkplain ResponseInterceptor} from the {@linkplain Builder}
         *
         * @param interceptor The {@linkplain ResponseInterceptor} to be removed from the {@linkplain Builder}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain ResponseInterceptor} argument
         */
        public Builder removeInterceptor(ResponseInterceptor interceptor) {
            if (interceptor == null) {
                throw new IllegalArgumentException("ResponseInterceptor argument cannot be null.");
            }
            this.interceptors.remove(interceptor);
            return this;
        }

        /**
         * Removes a {@linkplain Collection} of {@linkplain ResponseInterceptor} from the {@linkplain Builder}
         * <p>
         * Does the same thing as {@linkplain #removeInterceptor(ResponseInterceptor)}
         * for each {@linkplain ResponseInterceptor} in the {@linkplain Collection}
         *
         * @param interceptors A {@linkplain Collection} of {@linkplain ResponseInterceptor}
         *                     objects to be removed from the {@linkplain Builder}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain Collection} argument
         */
        public Builder removeInterceptors(Collection<ResponseInterceptor> interceptors) {
            if (interceptors == null) {
                throw new IllegalArgumentException("ResponseInterceptor collection argument cannot be null.");
            }
            for (ResponseInterceptor r : interceptors) {
                removeInterceptor(r);
            }
            return this;
        }

        /**
         * Adds a {@linkplain Collection} of {@linkplain ResponseInterceptor} objects for the {@linkplain Builder}
         * <p>
         * Does the same thing as {@linkplain #addInterceptor(ResponseInterceptor)} for each
         * {@linkplain ResponseInterceptor} in the {@linkplain Collection}
         *
         * @param interceptors A {@linkplain Collection} of {@linkplain ResponseInterceptor}
         *                     objects to be added to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         */
        public Builder addInterceptors(Collection<ResponseInterceptor> interceptors) {
            if (interceptors == null) {
                throw new IllegalArgumentException("ResponseInterceptor collection argument cannot be null.");
            }
            for (ResponseInterceptor r : interceptors) {
                addInterceptor(r);
            }
            return this;
        }

        /**
         * Sets a single {@linkplain CallAdapter.Factory} for this {@linkplain ApiComposer}
         *
         * @param adapterFactory The {@linkplain CallAdapter.Factory} to be set to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain CallAdapter.Factory} argument
         */
        public Builder addAdapterFactory(CallAdapter.Factory adapterFactory) {
            if (adapterFactory == null) {
                throw new IllegalArgumentException("CallAdapter.Factory argument cannot be null.");
            }
            this.callAdapterFactories.add(adapterFactory);
            return this;
        }

        /**
         * Adds an array of {@linkplain CallAdapter.Factory} objects for the {@linkplain Builder}
         * <p>
         * Does the same thing as {@linkplain #addAdapterFactory(CallAdapter.Factory)} for each
         * {@linkplain CallAdapter.Factory} in the {@linkplain Collection}
         *
         * @param adapterFactories An array of {@linkplain CallAdapter.Factory}
         *                     objects to be added to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         */
        public Builder addAdapterFactories(CallAdapter.Factory... adapterFactories) {
            if (adapterFactories == null) {
                throw new IllegalArgumentException("CallAdapter.Factory collection argument cannot be null.");
            }
            return addAdapterFactories(Arrays.asList(adapterFactories));
        }

        /**
         * Adds a {@linkplain Collection} of {@linkplain CallAdapter.Factory} objects for the {@linkplain Builder}
         * <p>
         * Does the same thing as {@linkplain #addAdapterFactory(CallAdapter.Factory)} for each
         * {@linkplain CallAdapter.Factory} in the {@linkplain Collection}
         *
         * @param adapterFactories A {@linkplain Collection} of {@linkplain CallAdapter.Factory}
         *                     objects to be added to the {@linkplain ApiComposer}
         * @return A reference to the {@linkplain Builder} object
         */
        public Builder addAdapterFactories(Iterable<CallAdapter.Factory> adapterFactories) {
            if (adapterFactories == null) {
                throw new IllegalArgumentException("CallAdapter.Factory collection argument cannot be null.");
            }
            for (CallAdapter.Factory factory : adapterFactories) {
                addAdapterFactory(factory);
            }
            return this;
        }

        /**
         * Determine if the interface implementation will be lazy or eager.
         * <p>
         * If set to {@code true }the {@linkplain ApiComposer} created from this builder will initialize
         * all interface method implementations immediately after the {@linkplain ApiComposer#compose(Class)} is called.
         * <p>
         * If set to {@code false} the {@linkplain ApiComposer#compose(Class)} will only
         * basic conditions for the supplied interface class and will actually resolve
         * the interface method implementations on-demand (when the methods are called for the first time).
         *
         * @param loadEagerly the condition for whether the interface implementation should be lazy or eager
         * @return A reference to the {@linkplain Builder} object
         */
        public Builder loadEagerly(boolean loadEagerly) {
            this.loadEagerly = loadEagerly;
            return this;
        }

        /**
         * Creates and returns a new instance of the {@linkplain ApiComposer}
         * with the parameters set via the {@linkplain Builder}
         *
         * @return A new instance of the {@linkplain ApiComposer} with the parameters set via the {@linkplain Builder}
         */
        public ApiComposer create() {
            return new ApiComposer(this);
        }
    }
}
