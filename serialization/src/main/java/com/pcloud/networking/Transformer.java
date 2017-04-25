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
import java.util.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Transformer {

    private final static Collection<TypeAdapterFactory> DEFAULT_FACTORIES;

    static {
        DEFAULT_FACTORIES = new ArrayList<>(4);
        DEFAULT_FACTORIES.add(new PrimitiveTypesAdapterFactory());
        DEFAULT_FACTORIES.add(new CollectionsTypeAdapterFactory());
        DEFAULT_FACTORIES.add(new ArrayTypeAdapterFactory());
        DEFAULT_FACTORIES.add(new ClassTypeAdapterFactory());
    }

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

    public <T> TypeAdapter<T> getTypeAdapter(Class<T> type) {
        return getTypeAdapter((Type)type);
    }

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

    public Builder newBuilder() {
        List<TypeAdapterFactory> customFactories = new ArrayList<>(adapterFactories);
        customFactories.removeAll(DEFAULT_FACTORIES);
        return new Builder(customFactories);
    }

    public static class Builder {

        private List<TypeAdapterFactory> factories;

        private Builder() {
            factories = new ArrayList<>();
        }

        private Builder(List<TypeAdapterFactory> factories) {
            this.factories = new ArrayList<>(factories);
        }

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

        public Builder addTypeAdapterFactory(TypeAdapterFactory adapterFactory) {
            if (adapterFactory == null) {
                throw new IllegalArgumentException("TypeAdapterFactory cannot be null.");
            }

            factories.add(adapterFactory);
            return this;
        }

        public Transformer build() {
            return new Transformer(this);
        }

    }
}
