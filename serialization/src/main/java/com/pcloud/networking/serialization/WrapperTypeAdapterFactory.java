package com.pcloud.networking.serialization;

import java.lang.reflect.Type;

class WrapperTypeAdapterFactory implements TypeAdapterFactory {
    private final Type type;
    private final TypeAdapter<?> adapter;

    WrapperTypeAdapterFactory(Type type, TypeAdapter<?> adapter) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null.");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("TypeAdapter argument cannot be null.");
        }

        this.type = type;
        this.adapter = adapter;
    }

    @Override
    public TypeAdapter<?> create(Type requested, Transformer transformer) {
        return type.equals(requested) ? adapter : null;
    }
}
