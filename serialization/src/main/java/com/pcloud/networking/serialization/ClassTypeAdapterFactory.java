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

import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.protocol.SerializationException;
import com.pcloud.utils.Types;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

class ClassTypeAdapterFactory implements TypeAdapterFactory {

    static final TypeAdapterFactory INSTANCE = new ClassTypeAdapterFactory();

    private ClassTypeAdapterFactory() {
    }

    @Override
    public TypeAdapter<?> create(Type type, Transformer transformer) {
        Class<?> rawType = Types.getRawType(type);
        if (rawType.isInterface() || rawType.isEnum()) return null;
        if (isPlatformType(rawType) && !rawType.isPrimitive() && !rawType.isEnum()) {
            throw new IllegalArgumentException("Platform type '" + type +
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

        ClassFactory<?> classFactory = ClassFactory.get(rawType);
        Map<String, ClassTypeAdapter.Binding> fields = new HashMap<>();
        for (Type t = type; t != Object.class; t = Types.getGenericSuperclass(t)) {
            createFieldBindings(transformer, t, fields);
        }
        return new ClassTypeAdapter<>(classFactory, fields);
    }

    /**
     * Creates a field binding for each of declared field of {@code type}.
     */
    private void createFieldBindings(Transformer transformer, Type type,
                                     Map<String, ClassTypeAdapter.Binding> fieldBindings) {
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
            field.setAccessible(true);

            // Determine the serialized parameter name, fail if the name is already used.
            String annotatedName = paramAnnotation.value();
            String name = annotatedName.equals(ParameterValue.DEFAULT_NAME) ? field.getName() : annotatedName;
            ClassTypeAdapter.Binding fieldBinding = createBindingForField(transformer, name, field, fieldType);
            ClassTypeAdapter.Binding existing = fieldBindings.put(name, fieldBinding);
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

    private static ClassTypeAdapter.Binding createPrimitiveBinding(String name, Field field,
                                                                   Class<?> fieldClass) {
        if (fieldClass == long.class) {
            return new LongBinding(name, field);
        } else if (fieldClass == int.class) {
            return new IntBinding(name, field);
        } else if (fieldClass == float.class) {
            return new FloatBinding(name, field);
        } else if (fieldClass == double.class) {
            return new DoubleBinding(name, field);
        } else if (fieldClass == short.class) {
            return new ShortBinding(name, field);
        } else if (fieldClass == byte.class) {
            return new ByteBinding(name, field);
        } else if (fieldClass == boolean.class) {
            return new BooleanBinding(name, field);
        } else if (fieldClass == char.class) {
            return new CharBinding(name, field);
        } else {
            throw new IllegalArgumentException("Cannot serialize value of type '" + fieldClass.getName() + "'.");
        }
    }

    private static ClassTypeAdapter.Binding createBindingForField(Transformer transformer, String name, Field field,
                                                                  Type fieldType) {
        Class<?> fieldClass = Types.getRawType(fieldType);
        if (fieldClass.isPrimitive()) {
            return createPrimitiveBinding(name, field, fieldClass);
        } else {
            TypeAdapter<Object> adapter = transformer.getTypeAdapter(fieldType);
            if (!fieldClass.isPrimitive() || !fieldClass.isEnum()) {
                adapter = new GuardedSerializationTypeAdapter<>(adapter);
            }

            return new ObjectBinding<>(name, field, adapter);
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


    private static class ObjectBinding<T> extends ClassTypeAdapter.Binding {
        final TypeAdapter<T> adapter;

        ObjectBinding(String name, Field field, TypeAdapter<T> adapter) {
            super(name, field);
            this.adapter = adapter;
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            try {
                T fieldValue = adapter.deserialize(reader);
                field.set(target, fieldValue);
            } catch (SerializationException e) {
                throw new SerializationException("Cannot deserialize field '" +
                        field.getDeclaringClass().getName() +
                        "." +
                        field.getName() +
                        "'" +
                        " of type '" +
                        field.getType().getName() +
                        "'.",
                        e);
            }
        }

        @SuppressWarnings("unchecked")
        //Field's values are of type T.
        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            T fieldValue = (T) field.get(target);
            if (fieldValue != null) {
                writer.writeName(name);
                adapter.serialize(writer, fieldValue);
            }
        }
    }

    private static class BooleanBinding extends ClassTypeAdapter.Binding {

        BooleanBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            field.set(target, reader.readBoolean());
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            boolean fieldValue = field.getBoolean(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }

    private static class ByteBinding extends ClassTypeAdapter.Binding {

        ByteBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            field.setByte(target, (byte) reader.readNumber());
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            byte fieldValue = field.getByte(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }

    private static class CharBinding extends ClassTypeAdapter.Binding {

        CharBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            field.setChar(target, (char) reader.readNumber());
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            char fieldValue = field.getChar(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }

    private static class ShortBinding extends ClassTypeAdapter.Binding {

        ShortBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            field.setShort(target, (short) reader.readNumber());
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            short fieldValue = field.getShort(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }

    private static class DoubleBinding extends ClassTypeAdapter.Binding {

        DoubleBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            String numberString = reader.readString();
            try {
                field.setDouble(target, Double.parseDouble(numberString));
            } catch (NumberFormatException e) {
                throw new IOException("Failed to convert '" + numberString + "' to a Float", e);
            }
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            double fieldValue = field.getDouble(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }

    private static class FloatBinding extends ClassTypeAdapter.Binding {

        FloatBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            String numberString = reader.readString();
            try {
                field.setFloat(target, Float.parseFloat(numberString));
            } catch (NumberFormatException e) {
                throw new IOException("Failed to convert '" + numberString + "' to a Float", e);
            }
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            float fieldValue = field.getFloat(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }

    private static class IntBinding extends ClassTypeAdapter.Binding {

        IntBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            field.setInt(target, (int) reader.readNumber());
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            int fieldValue = field.getInt(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }

    private static class LongBinding extends ClassTypeAdapter.Binding {

        LongBinding(String name, Field field) {
            super(name, field);
        }

        @Override
        void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException {
            field.setLong(target, reader.readNumber());
        }

        @Override
        void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException {
            long fieldValue = field.getLong(target);
            writer.writeName(name);
            writer.writeValue(fieldValue);
        }
    }
}
