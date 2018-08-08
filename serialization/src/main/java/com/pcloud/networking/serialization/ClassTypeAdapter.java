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

import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

class ClassTypeAdapter<T> extends TypeAdapter<T> {
    private final ClassFactory<T> classFactory;
    private final Map<String, Binding> nameToBindingMap;

    ClassTypeAdapter(ClassFactory<T> classFactory, Map<String, Binding> fieldsMap) {
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
                Binding binding = nameToBindingMap.get(name);
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
            for (Binding binding : nameToBindingMap.values()) {
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

    abstract static class Binding {
        final String name;
        final Field field;

        Binding(String name, Field field) {
            this.name = name;
            this.field = field;
        }

        abstract void read(ProtocolReader reader, Object target) throws IOException, IllegalAccessException;

        abstract void write(ProtocolWriter writer, Object target) throws IllegalAccessException, IOException;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Binding)) return false;
            Binding binding = (Binding) o;
            return Objects.equals(name, binding.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
