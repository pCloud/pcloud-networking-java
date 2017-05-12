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

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * A {@linkplain TypeAdapterFactory} implementation for the core Java language primitive types.
 */
class PrimitiveTypesAdapterFactory implements TypeAdapterFactory {
    private static final TypeAdapter<String> STRING_ADAPTER = new TypeAdapter<String>() {
        @Override
        public String deserialize(ProtocolReader reader) throws IOException {
            return reader.readString();
        }

        @Override
        public void serialize(ProtocolWriter writer, String value) throws IOException {
            if (value != null) {
                writer.writeValue(value);
            }
        }
    };

    private static final TypeAdapter<Boolean> BOOLEAN_ADAPTER = new TypeAdapter<Boolean>() {
        @Override
        public Boolean deserialize(ProtocolReader reader) throws IOException {
            return reader.readBoolean();
        }

        @Override
        public void serialize(ProtocolWriter writer, Boolean value) throws IOException {
            writer.writeValue((boolean) value);
        }
    };

    private static final TypeAdapter<Integer> INTEGER_ADAPTER = new TypeAdapter<Integer>() {
        @Override
        public Integer deserialize(ProtocolReader reader) throws IOException {
            return (int) reader.readNumber();
        }

        @Override
        public void serialize(ProtocolWriter writer, Integer value) throws IOException {
            if (value != null) {
                writer.writeValue((int) value);
            }
        }
    };

    private static final TypeAdapter<Long> LONG_ADAPTER = new TypeAdapter<Long>() {
        @Override
        public Long deserialize(ProtocolReader reader) throws IOException {
            return reader.readNumber();
        }

        @Override
        public void serialize(ProtocolWriter writer, Long value) throws IOException {
            if (value != null) {
                writer.writeValue((long) value);
            }
        }
    };

    private static final TypeAdapter<Double> DOUBLE_ADAPTER = new TypeAdapter<Double>() {
        @Override
        public Double deserialize(ProtocolReader reader) throws IOException {
            String numberString = reader.readString();
            try {
                return Double.parseDouble(numberString);
            } catch (NumberFormatException e) {
                throw new IOException("Failed to convert '" + numberString + "' to a Double", e);
            }
        }

        @Override
        public void serialize(ProtocolWriter writer, Double value) throws IOException {
            if (value != null) {
                writer.writeValue((double) value);
            }
        }
    };

    private static final TypeAdapter<Float> FLOAT_ADAPTER = new TypeAdapter<Float>() {
        @Override
        public Float deserialize(ProtocolReader reader) throws IOException {
            String numberString = reader.readString();
            try {
                return Float.parseFloat(numberString);
            } catch (NumberFormatException e) {
                throw new IOException("Failed to convert '" + numberString + "' to a Float", e);
            }
        }

        @Override
        public void serialize(ProtocolWriter writer, Float value) throws IOException {
            if (value != null) {
                writer.writeValue((float) value);
            }
        }
    };

    private static final TypeAdapter<Short> SHORT_ADAPTER = new TypeAdapter<Short>() {
        @Override
        public Short deserialize(ProtocolReader reader) throws IOException {
            return (short) reader.readNumber();
        }

        @Override
        public void serialize(ProtocolWriter writer, Short value) throws IOException {
            if (value != null) {
                writer.writeValue((short) value);
            }
        }
    };

    private static final TypeAdapter<Byte> BYTE_ADAPTER = new TypeAdapter<Byte>() {
        @Override
        public Byte deserialize(ProtocolReader reader) throws IOException {
            return (byte) reader.readNumber();
        }

        @Override
        public void serialize(ProtocolWriter writer, Byte value) throws IOException {
            if (value != null) {
                writer.writeValue((byte) value);
            }
        }
    };

    @SuppressWarnings("unchecked")
    @Override
    public TypeAdapter<?> create(Type type, Transformer transformer) {
        if (type == String.class) return STRING_ADAPTER;
        if (type == long.class || type == Long.class) return LONG_ADAPTER;
        if (type == boolean.class || type == Boolean.class) return BOOLEAN_ADAPTER;
        if (type == double.class || type == Double.class) return DOUBLE_ADAPTER;
        if (type == float.class || type == Float.class) return FLOAT_ADAPTER;
        if (type == int.class || type == Integer.class) return INTEGER_ADAPTER;
        if (type == short.class || type == Short.class) return SHORT_ADAPTER;
        if (type == byte.class || type == Byte.class) return BYTE_ADAPTER;

        Class<?> rawType = Types.getRawType(type);
        if (rawType.isEnum()) {
            return new EnumTypeAdapter<>((Class<? extends Enum>) rawType);
        }

        return null;
    }
}
