package com.pcloud.networking;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

import static com.pcloud.networking.Util.typesMatch;

public class Cyclone {

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

    private Cyclone(Builder builder) {
        typeToAdapterMap = new LinkedHashMap<>();
        pendingAdapterRef = new ThreadLocal<>();
        adapterFactories = new ArrayList<>();
        adapterFactories.addAll(DEFAULT_FACTORIES);
        adapterFactories.addAll(builder.factories);
    }

    public <T> TypeAdapter<T> getTypeAdapter(Class<T> type) {
        return getTypeAdapter((Type)type);
    }

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
                TypeAdapter<T> result = (TypeAdapter<T>) factory.create(type, Collections.<Annotation>emptySet(), this);
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
                public TypeAdapter<?> create(Type requested, Set<? extends Annotation> annotations, Cyclone cyclone) {
                    return typesMatch(type, requested) ? adapter : null;
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

        public Cyclone build() {
            return new Cyclone(this);
        }

    }
}
