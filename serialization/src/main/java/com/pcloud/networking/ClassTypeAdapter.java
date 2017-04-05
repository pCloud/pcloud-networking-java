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

import com.pcloud.protocol.streaming.ProtocolReader;
import com.pcloud.protocol.streaming.ProtocolWriter;
import com.pcloud.protocol.streaming.SerializationException;
import com.pcloud.protocol.streaming.TypeToken;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

class ClassTypeAdapter<T> extends TypeAdapter<T> {
    private final ClassFactory<T> classFactory;
    private final Map<String, Binding<?>> nameToBindingMap;

    ClassTypeAdapter(ClassFactory<T> classFactory, Map<String, Binding<?>> fieldsMap) {
        this.classFactory = classFactory;
        this.nameToBindingMap = fieldsMap;
    }

    @Override
    public T deserialize(ProtocolReader reader) throws IOException {
        T result;
        try {
            result = classFactory.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException) throw (RuntimeException) targetException;
            if (targetException instanceof Error) throw (Error) targetException;
            throw new RuntimeException(targetException);
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        }

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.readString();
                Binding<?> binding = nameToBindingMap.get(name);
                if (binding != null) {
                    binding.read(reader, result);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return result;
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        }
    }

    @Override
    public void serialize(ProtocolWriter writer, T value) throws IOException {
        try {
            for (Binding<?> binding : nameToBindingMap.values()) {
                binding.write(writer, value);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return "TypeAdapter[" + classFactory.getClass().getName() + "]";
    }

    static class Binding<T> {
        final String name;
        final Field field;
        final TypeAdapter<T> adapter;
        final boolean canBeSerialized;

        Binding(String name, Field field, TypeAdapter<T> adapter, boolean canBeSerialized) {
            this.name = name;
            this.field = field;
            this.adapter = adapter;
            this.canBeSerialized = canBeSerialized;
        }

        void read(ProtocolReader reader, Object value) throws IOException, IllegalAccessException {
            try {
                T fieldValue = adapter.deserialize(reader);
                field.set(value, fieldValue);
            } catch (SerializationException e) {
                throw new SerializationException("Failed to deserialize field '" + name + "'" +
                        " of type '"+field.getType().getName()+
                        "' in class '"+field.getDeclaringClass().getName()+"'.", e);
            }
        }

        @SuppressWarnings("unchecked")
            // We require that field's values are of type T.
        void write(ProtocolWriter writer, Object value) throws IllegalAccessException, IOException {
            if (canBeSerialized) {
                writer.writeName(name, guessFieldType(value));
                T fieldValue = (T) field.get(value);
                adapter.serialize(writer, fieldValue);
            }
        }

        private TypeToken guessFieldType(Object value) throws IOException {
            Class<?> type = value.getClass();
            if (type.isAssignableFrom(Number.class)) {
                if (type == Long.class || type == Integer.class || type == Short.class || type == Byte.class) {
                    long numberValue = (long) value;
                    return numberValue > 0 ? TypeToken.NUMBER : TypeToken.STRING;
                } else if (type == Double.class || type == Float.class) {
                    return TypeToken.STRING;
                }
            }

            if (type == String.class) {
                return TypeToken.STRING;
            } else if (type == Boolean.class) {
                return TypeToken.BOOLEAN;
            } else {
                throw new SerializationException("Cannot serialize field '" + name + "' value of type '" + type.getName() + "'.");
            }
        }
    }
}
