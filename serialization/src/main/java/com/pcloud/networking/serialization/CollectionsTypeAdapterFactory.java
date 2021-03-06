/*
 * Copyright (C) 2017 pCloud AG.
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

package com.pcloud.networking.serialization;

import com.pcloud.utils.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class CollectionsTypeAdapterFactory implements TypeAdapterFactory {

    static final TypeAdapterFactory INSTANCE = new CollectionsTypeAdapterFactory();

    private CollectionsTypeAdapterFactory() {
    }

    @Override
    public TypeAdapter<?> create(Type type, Transformer transformer) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        Class<?> rawType = Types.getRawType(type);
        if (rawType.isAssignableFrom(List.class)) {
            return newArrayListAdapter(type, transformer);
        } else if (rawType == Set.class) {
            return newLinkedHashSetAdapter(type, transformer);
        } else if (rawType == Map.class) {
            return newMapAdapter((ParameterizedType) type, transformer);
        }
        return null;
    }

    private static <T> TypeAdapter<Collection<T>> newArrayListAdapter(Type type, Transformer transformer) {
        Type elementType = Types.collectionElementType(type, Collection.class);
        TypeAdapter<T> elementAdapter = getTypeAdapter(transformer, elementType);
        return new CollectionTypeAdapter<Collection<T>, T>(elementAdapter) {
            @Override
            protected Collection<T> instantiateCollection() {
                return new ArrayList<>();
            }
        };
    }

    private static <T> TypeAdapter<Set<T>> newLinkedHashSetAdapter(Type type, Transformer transformer) {
        Type elementType = Types.collectionElementType(type, Collection.class);
        TypeAdapter<T> elementAdapter = getTypeAdapter(transformer, elementType);
        return new CollectionTypeAdapter<Set<T>, T>(elementAdapter) {

            @Override
            protected Set<T> instantiateCollection() {
                return new LinkedHashSet<>();
            }
        };
    }

    private static <K, V> TypeAdapter<Map<K, V>> newMapAdapter(ParameterizedType type, Transformer transformer) {
        Type keyElementType = Types.getParameterUpperBound(0, type);
        Type valueElementType = Types.getParameterUpperBound(1, type);
        TypeAdapter<K> keyAdapter = transformer.getTypeAdapter(keyElementType);
        TypeAdapter<V> valueAdapter = transformer.getTypeAdapter(valueElementType);
        return new MapTypeAdapter<K, V>(keyAdapter, valueAdapter) {
            @Override
            protected Map<K, V> instantiateCollection() {
                return new TreeMap<>();
            }
        };
    }

    private static <T> TypeAdapter<T> getTypeAdapter(Transformer transformer, Type type) {
        TypeAdapter<T> typeAdapter = transformer.getTypeAdapter(type);

        Class<?> rawType = Types.getRawType(type);
        if (!rawType.isPrimitive() || !rawType.isEnum()) {
            typeAdapter = new GuardedSerializationTypeAdapter<>(typeAdapter);
        }
        return typeAdapter;
    }
}
