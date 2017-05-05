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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A factory like class which provides {@linkplain TypeAdapter} objects
 * <p>
 * Provides {@linkplain TypeAdapter} objects which can serialize/deserialize the binary data
 * to/from objects or {@linkplain Map} for network requests.
 * Contains {@linkplain TypeAdapter} objects for default java data types and accepts custom {@linkplain TypeAdapter}
 * and {@linkplain TypeAdapterFactory} objects for user specific deserialization behaviour.
 *
 * @see TypeAdapter
 * @see TypeAdapterFactory
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Transformer {

    private static final Collection<TypeAdapterFactory> DEFAULT_FACTORIES;
    private static final int DEFAULT_FACTORIES_LIST_CAPACITY = 4;
    static {
        DEFAULT_FACTORIES = new ArrayList<>(DEFAULT_FACTORIES_LIST_CAPACITY);
        DEFAULT_FACTORIES.add(new PrimitiveTypesAdapterFactory());
        DEFAULT_FACTORIES.add(new CollectionsTypeAdapterFactory());
        DEFAULT_FACTORIES.add(new ArrayTypeAdapterFactory());
        DEFAULT_FACTORIES.add(new ClassTypeAdapterFactory());
    }

    /**
     * Create an instance of a {@linkplain Builder} with which you can create the {@linkplain Transformer}
     *
     * @return A {@linkplain Builder} instance
     */
    public static Builder create() {
        return new Builder();
    }

    private final List<TypeAdapterFactory> adapterFactories;
    private final Map<Type, TypeAdapter<?>> typeToAdapterMap;
    private final ThreadLocal<List<StubTypeAdapter<?>>> pendingAdapterRef;

    private Transformer(Builder builder) {
        typeToAdapterMap = new LinkedHashMap<>();
        pendingAdapterRef = new ThreadLocal<>();
        adapterFactories = new ArrayList<>();
        adapterFactories.addAll(builder.factories);
        adapterFactories.addAll(DEFAULT_FACTORIES);
    }

    /**
     * Provides a {@linkplain TypeAdapter} object which can work with a given type of data
     *
     * @param type The type of data the adapter will be working with
     * @param <T>  The concrete java representation of the type of data
     * @return A {@linkplain TypeAdapter} which can serialize/deserialize the data type provided as an argument
     * @throws IllegalStateException if it is not possible to create an adapter for a given type
     */
    public <T> TypeAdapter<T> getTypeAdapter(Class<T> type) {
        return getTypeAdapter((Type) type);
    }

    /**
     * Provides a {@linkplain TypeAdapter} for a given {@linkplain Type} of data
     *
     * @param type A {@linkplain Type} representing the type of data
     * @param <T>  The concrete java representation of the data type
     * @return A {@linkplain TypeAdapter} which can serialize/deserialise the data type provided as an argument
     * @throws IllegalStateException if it is not possible to create an adapter for a given type
     */
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> getTypeAdapter(Type type) {
        type = Types.canonicalize(type);

        // Try to return a cached adapter, if any.
        synchronized (typeToAdapterMap) {
            TypeAdapter<?> result = typeToAdapterMap.get(type);
            if (result != null) {
                return (TypeAdapter<T>) result;
            }
        }


        // Check for any pending cyclic adapters
        List<StubTypeAdapter<?>> pendingAdapters = pendingAdapterRef.get();
        if (pendingAdapters != null) {
            for (StubTypeAdapter<?> adapter : pendingAdapters) {
                if (adapter.cacheKey.equals(type)) {
                    // There's a pending adapter, return the stub to
                    // avoid an endless recursion.
                    return (TypeAdapter<T>) adapter;
                }
            }
        } else {
            pendingAdapters = new ArrayList<>();
            pendingAdapterRef.set(pendingAdapters);
        }

        // Create a stub adapter for the requested type.
        StubTypeAdapter<T> adapterStub = new StubTypeAdapter<>(type);
        pendingAdapters.add(adapterStub);

        // Iterate though the factories and create an adapter for the type.
        try {
            for (TypeAdapterFactory factory : adapterFactories) {
                @SuppressWarnings("unchecked")
                TypeAdapter<T> result = (TypeAdapter<T>) factory.create(type, this);
                if (result != null) {
                    adapterStub.setDelegate(result);
                    synchronized (typeToAdapterMap) {
                        typeToAdapterMap.put(type, result);
                    }
                    return result;
                }
            }
        } finally {
            pendingAdapters.remove(pendingAdapters.size() - 1);
            if (pendingAdapters.isEmpty()) {
                pendingAdapterRef.remove();
            }
        }

        throw new IllegalStateException("Cannot create an adapter for type '" + type + "'.");
    }

    /**
     * Create a new instance of the {@linkplain Builder}
     * <p>
     * The new builder will inherit all the custom {@linkplain TypeAdapterFactory} objects of the current {@linkplain Transformer}
     *
     * @return A new {@linkplain Builder} instance
     */
    public Builder newBuilder() {
        List<TypeAdapterFactory> customFactories = new ArrayList<>(adapterFactories);
        customFactories.removeAll(DEFAULT_FACTORIES);
        return new Builder(customFactories);
    }

    /**
     * Builds {@linkplain Transformer} instances
     * <p>
     * The builder can add custom {@linkplain TypeAdapter} and {@linkplain TypeAdapterFactory} to the {@linkplain Transformer}
     */
    public static class Builder {

        private List<TypeAdapterFactory> factories;

        private Builder() {
            factories = new ArrayList<>();
        }

        private Builder(List<TypeAdapterFactory> factories) {
            this.factories = new ArrayList<>(factories);
        }

        /**
         * Adds a custom {@linkplain TypeAdapter} to the {@linkplain Transformer}
         *
         * @param type    The data type that this adapter can work with
         * @param adapter The {@linkplain TypeAdapter} to be added to the {@linkplain Transformer}
         * @param <T>     The concrete java representation of the data type
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on null arguments
         */
        public <T> Builder addTypeAdapter(final Type type, final TypeAdapter<T> adapter) {
            if (type == null) {
                throw new IllegalArgumentException("Type argument cannot be null.");
            }
            if (adapter == null) {
                throw new IllegalArgumentException("TypeAdapter argument cannot be null.");
            }

            factories.add(new TypeAdapterFactory() {
                @Override
                public TypeAdapter<?> create(Type requested, Transformer transformer) {
                    return type.equals(requested) ? adapter : null;
                }
            });

            return this;
        }

        /**
         * Adds a custom {@linkplain TypeAdapterFactory} to the {@linkplain Transformer}
         *
         * @param adapterFactory The {@linkplain TypeAdapterFactory} to be added to the {@linkplain Transformer}
         * @return A reference ot the {@linkplain Builder} object
         * @throws IllegalArgumentException on null arguments
         */
        public Builder addTypeAdapterFactory(TypeAdapterFactory adapterFactory) {
            if (adapterFactory == null) {
                throw new IllegalArgumentException("TypeAdapterFactory cannot be null.");
            }

            factories.add(adapterFactory);
            return this;
        }

        /**
         * Builds the {@linkplain Transformer} object with all the custom {@linkplain TypeAdapter}
         * and {@linkplain TypeAdapterFactory} objects added to the {@linkplain Builder}
         *
         * @return A new instance of the {@linkplain Transformer} object
         */
        public Transformer build() {
            return new Transformer(this);
        }

    }
}
