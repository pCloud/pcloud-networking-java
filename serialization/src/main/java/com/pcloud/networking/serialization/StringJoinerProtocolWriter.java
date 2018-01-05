/*
 * Copyright (c) 2018 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.networking.serialization;

import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.protocol.SerializationException;

import java.io.IOException;

class StringJoinerProtocolWriter implements ProtocolWriter {

    private StringBuilder builder;
    private final String delimiter;
    private int valueCount;

    StringJoinerProtocolWriter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + result() + "]";
    }

    public String result() {
        return builder != null ? builder.toString() : "";
    }

    public void reset() {
        valueCount = 0;
        if (builder != null) {
            builder.setLength(0);
        }
    }

    @Override
    public ProtocolWriter writeName(String name) throws IOException {
        throw new SerializationException("Object must serialize to a single value.");
    }

    @Override
    public ProtocolWriter writeValue(Object value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Value argument cannot be null");
        }

        if (!value.getClass().isPrimitive()) {
            throw new IllegalArgumentException("Cannot serialize value of type '" + value.getClass() + "'.");
        }

        return writeValue(String.valueOf(value));
    }

    @Override
    public ProtocolWriter writeValue(String value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Value argument cannot be null");
        }

        if (builder == null) {
            builder = new StringBuilder();
        }
        if (valueCount > 0) {
            builder.append(delimiter);
        }
        builder.append(value);
        valueCount++;
        return this;
    }

    @Override
    public ProtocolWriter writeValue(double value) throws IOException {
        return writeValue(String.valueOf(value));
    }

    @Override
    public ProtocolWriter writeValue(float value) throws IOException {
        return writeValue(String.valueOf(value));
    }

    @Override
    public ProtocolWriter writeValue(long value) throws IOException {
        return writeValue(String.valueOf(value));
    }

    @Override
    public ProtocolWriter writeValue(boolean value) throws IOException {
        return writeValue(String.valueOf(value));
    }

    @Override
    public void close() {
    }
}
