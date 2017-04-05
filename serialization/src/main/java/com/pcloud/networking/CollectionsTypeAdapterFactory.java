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

package com.pcloud.networking;

import java.lang.reflect.Type;
import java.util.*;

class CollectionsTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public TypeAdapter<?> create(Type type, Transformer transformer) {
        Class<?> rawType = Types.getRawType(type);
        if (rawType.isAssignableFrom(List.class)) {
            return newArrayListAdapter(type, transformer);
        } else if (rawType == Set.class) {
            return newLinkedHashSetAdapter(type, transformer);
        }
        return null;
    }

    static <T> TypeAdapter<Collection<T>> newArrayListAdapter(Type type, Transformer transformer) {
        Type elementType = Types.collectionElementType(type, Collection.class);
        TypeAdapter<T> elementAdapter = transformer.getTypeAdapter(elementType);
        return new CollectionTypeAdapter<Collection<T>, T>(elementAdapter) {
            @Override
            protected Collection<T> instantiateCollection() {
                return new ArrayList<>();
            }
        };
    }

    static <T> TypeAdapter<Set<T>> newLinkedHashSetAdapter(Type type, Transformer transformer) {
        Type elementType = Types.collectionElementType(type, Collection.class);
        TypeAdapter<T> elementAdapter = transformer.getTypeAdapter(elementType);
        return new CollectionTypeAdapter<Set<T>, T>(elementAdapter) {

            @Override
            protected Set<T> instantiateCollection() {
                return new LinkedHashSet<>();
            }
        };
    }
}
