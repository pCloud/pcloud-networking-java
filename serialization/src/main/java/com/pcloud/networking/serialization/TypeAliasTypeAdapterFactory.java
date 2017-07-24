package com.pcloud.networking.serialization;

import com.pcloud.utils.Types;

import java.lang.reflect.Type;

class TypeAliasTypeAdapterFactory implements TypeAdapterFactory {

    private Type targetType;
    private Type aliasType;

    TypeAliasTypeAdapterFactory(Type targetType, Type aliasType) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type argument cannot be null.");
        }

        if (aliasType == null) {
            throw new IllegalArgumentException("Alias type argument cannot be null.");
        }

        if (Types.getRawType(targetType).equals(Types.getRawType(aliasType))) {
            throw new IllegalArgumentException("Target and alias type argument refer to the same type.");
        }

        this.targetType = targetType;
        this.aliasType = aliasType;
    }

    @Override
    public TypeAdapter<?> create(Type type, Transformer transformer) {
        if (targetType.equals(type)) {
            return transformer.getTypeAdapter(aliasType);
        }

        return null;
    }
}
