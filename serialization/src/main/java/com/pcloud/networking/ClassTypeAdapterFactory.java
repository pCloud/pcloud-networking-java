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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class ClassTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public TypeAdapter<?> create(Type type, Set<? extends Annotation> annotations, Cyclone moshi) {
        Class<?> rawType = Types.getRawType(type);
        if (rawType.isInterface() || rawType.isEnum()) return null;
        if (isPlatformType(rawType) && !Types.isAllowedPlatformType(rawType)) {
            throw new IllegalArgumentException("Platform "
                    + type
                    + " annotated "
                    + annotations
                    + " requires explicit JsonAdapter to be registered");
        }
        if (!annotations.isEmpty()) return null;

        if (rawType.getEnclosingClass() != null && !Modifier.isStatic(rawType.getModifiers())) {
            if (rawType.getSimpleName().isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot serialize anonymous class " + rawType.getName());
            } else {
                throw new IllegalArgumentException(
                        "Cannot serialize non-static nested class " + rawType.getName());
            }
        }
        if (Modifier.isAbstract(rawType.getModifiers())) {
            throw new IllegalArgumentException("Cannot serialize abstract class " + rawType.getName());
        }

        ClassFactory<Object> classFactory = ClassFactory.get(rawType);
        Map<String, ClassTypeAdapter.Binding<?>> fields = new TreeMap<>();
        for (Type t = type; t != Object.class; t = Types.getGenericSuperclass(t)) {
            createFieldBindings(moshi, t, fields);
        }
        return new ClassTypeAdapter<>(classFactory, fields);
    }

    /**
     * Creates a field binding for each of declared field of {@code type}.
     */
    private void createFieldBindings(
            Cyclone moshi, Type type, Map<String, ClassTypeAdapter.Binding<?>> fieldBindings) {
        Class<?> rawType = Types.getRawType(type);
        boolean platformType = isPlatformType(rawType);
        for (Field field : rawType.getDeclaredFields()) {
            if (!includeField(platformType, field.getModifiers())) continue;

            ParameterValue paramAnnotation = field.getAnnotation(ParameterValue.class);
            if (paramAnnotation == null) continue;

            // Look up a type adapter for this type.
            Type fieldType = Types.resolve(type, rawType, field.getGenericType());
            TypeAdapter<Object> adapter = moshi.getTypeAdapter(fieldType);

            // Create the binding between field and JSON.
            field.setAccessible(true);

            // Store it using the field's name. If there was already a field with this name, fail!
            String annotatedName = paramAnnotation.value();
            String name = annotatedName.equals(ParameterValue.DEFAULT_NAME) ? field.getName() : annotatedName;
            ClassTypeAdapter.Binding<Object> fieldBinding = new ClassTypeAdapter.Binding<>(name, field, adapter, fieldCanBeSerialized(field, fieldType));
            ClassTypeAdapter.Binding<?> replaced = fieldBindings.put(name, fieldBinding);
            if (replaced != null) {
                throw new IllegalArgumentException("Conflicting fields:\n"
                        + "    " + replaced.field + "\n"
                        + "    " + fieldBinding.field);
            }
        }
    }

    /**
     * Returns true if {@code rawType} is built in. We don't reflect on private fields of platform
     * types because they're unspecified and likely to be different on Java vs. Android.
     */
    private boolean isPlatformType(Class<?> rawType) {
        String name = rawType.getName();
        return name.startsWith("android.")
                || name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("kotlin.")
                || name.startsWith("scala.");
    }

    /**
     * Returns true if fields with {@code modifiers} are included in the emitted JSON.
     */
    private boolean includeField(boolean platformType, int modifiers) {
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) return false;
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || !platformType;
    }

    private boolean fieldCanBeSerialized(Field field, Type fieldType) {
        return fieldType == Boolean.class
                || fieldType == Byte.class
                || fieldType == Double.class
                || fieldType == Float.class
                || fieldType == Integer.class
                || fieldType == Long.class
                || fieldType == Short.class
                || fieldType == String.class;
    }
}
