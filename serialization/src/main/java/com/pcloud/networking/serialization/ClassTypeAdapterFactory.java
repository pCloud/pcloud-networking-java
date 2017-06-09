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

package com.pcloud.networking.serialization;

import com.pcloud.utils.Types;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

class ClassTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public TypeAdapter<?> create(Type type, Transformer transformer) {
        Class<?> rawType = Types.getRawType(type);
        if (rawType.isInterface() || rawType.isEnum()) return null;
        if (isPlatformType(rawType) && !Utils.isAllowedPlatformType(rawType)) {
            throw new IllegalArgumentException("Platform type '" +
                                                       type +
                                                       "' requires explicit TypeAdapter to be registered");
        }

        if (rawType.getEnclosingClass() != null && !Modifier.isStatic(rawType.getModifiers())) {
            if (rawType.getSimpleName().isEmpty()) {
                throw new IllegalArgumentException("Cannot serialize anonymous class " + rawType.getName());
            } else {
                throw new IllegalArgumentException("Cannot serialize non-static nested class " + rawType.getName());
            }
        }
        if (Modifier.isAbstract(rawType.getModifiers())) {
            throw new IllegalArgumentException("Cannot serialize abstract class " + rawType.getName());
        }

        ClassFactory<Object> classFactory = ClassFactory.get(rawType);
        Map<String, ClassTypeAdapter.Binding<?>> fields = new TreeMap<>();
        for (Type t = type; t != Object.class; t = Types.getGenericSuperclass(t)) {
            createFieldBindings(transformer, t, fields);
        }
        return new ClassTypeAdapter<>(classFactory, fields);
    }

    /**
     * Creates a field binding for each of declared field of {@code type}.
     */
    private void createFieldBindings(Transformer transformer, Type type,
                                     Map<String, ClassTypeAdapter.Binding<?>> fieldBindings) {
        Class<?> rawType = Types.getRawType(type);
        boolean platformType = isPlatformType(rawType);
        for (Field field : rawType.getDeclaredFields()) {
            if (!includeField(platformType, field.getModifiers())) continue;

            ParameterValue paramAnnotation = field.getAnnotation(ParameterValue.class);
            if (paramAnnotation == null) {
                // Skip all methods missing the annotation.
                continue;
            }

            Type fieldType = Types.resolve(type, rawType, field.getGenericType());
            TypeAdapter<Object> adapter = transformer.getTypeAdapter(fieldType);

            field.setAccessible(true);

            // Determine the serialized parameter name, fail if the name is already used.
            String annotatedName = paramAnnotation.value();
            String name = annotatedName.equals(ParameterValue.DEFAULT_NAME) ? field.getName() : annotatedName;
            ClassTypeAdapter.Binding<Object> fieldBinding =
                    new ClassTypeAdapter.Binding<>(name, field, adapter, Utils.fieldTypeIsSerializable(fieldType));
            ClassTypeAdapter.Binding<?> existing = fieldBindings.put(name, fieldBinding);
            if (existing != null) {
                throw new IllegalArgumentException("Conflicting fields:\n" +
                                                           "    " +
                                                           existing.field +
                                                           "\n" +
                                                           "    " +
                                                           fieldBinding.field);
            }
        }
    }

    /**
     * Returns true if {@code rawType} is built in. We don't reflect on private fields of platform
     * types because they're unspecified and likely to be different on Java vs. Android.
     */
    private boolean isPlatformType(Class<?> rawType) {
        String name = rawType.getName();
        return name.startsWith("android.") ||
                       name.startsWith("java.") ||
                       name.startsWith("javax.") ||
                       name.startsWith("kotlin.") ||
                       name.startsWith("scala.") ||
                       name.startsWith("groovy.");
    }

    /**
     * Returns true if fields with {@code modifiers} are included in the emitted JSON.
     */
    @SuppressWarnings("SimplifiableIfStatement")
    private boolean includeField(boolean platformType, int modifiers) {
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) return false;
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || !platformType;
    }
}
