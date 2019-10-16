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

import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.protocol.SerializationException;

import java.io.IOException;

/**
 * А {@linkplain TypeAdapter} implementation that enforces a "single" serialization rule
 * <p>
 * This implementation is useful for wrapping other {@linkplain TypeAdapter} instances and
 * ensuring that the objects supplied to the adapter will be serialized to a single value.
 *
 * @param <T> The type that this adapter can convert
 */
public class GuardedSerializationTypeAdapter<T> extends TypeAdapter<T> {

    private static final int GUARD_POOL_SIZE = 5;

    private TypeAdapter<T> delegate;
    private ObjectPool<SingleValueProtocolWriter> guardedWriterObjectPool = new ObjectPool<>(GUARD_POOL_SIZE);

    /**
     * Construct a new {@linkplain GuardedSerializationTypeAdapter} instance
     *
     * @param delegate a non-null {@linkplain TypeAdapter} to be guarded.
     * @throws IllegalArgumentException on a null {@linkplain TypeAdapter} argument.
     */
    public GuardedSerializationTypeAdapter(TypeAdapter<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("TypeAdapter argument cannot be null.");
        }
        this.delegate = delegate;
    }

    @Override
    public T deserialize(ProtocolReader reader) throws IOException {
        return delegate.deserialize(reader);
    }

    @Override
    public void serialize(ProtocolWriter writer, T value) throws IOException {
        SingleValueProtocolWriter singleValueProtocolWriter = guardedWriterObjectPool.acquire();
        if (singleValueProtocolWriter == null) {
            singleValueProtocolWriter = new SingleValueProtocolWriter();
        }
        singleValueProtocolWriter.guard(writer);
        try {
            delegate.serialize(singleValueProtocolWriter, value);
        } catch (NotSerializedToAValueException e) {
            throw new SerializationException(delegate.getClass() +
                    " does not serialize \"" + value.getClass() + "\" to a single value", e);
        } finally {
            singleValueProtocolWriter.reset();
            guardedWriterObjectPool.recycle(singleValueProtocolWriter);
        }
    }

    private static class NotSerializedToAValueException extends SerializationException {

        private NotSerializedToAValueException() {
            super("Object must serialize to a single value.");
        }
    }

    private static class SingleValueProtocolWriter implements ProtocolWriter {

        private ProtocolWriter delegate;

        void guard(ProtocolWriter writer) {
            delegate = writer;
        }

        void reset() {
            delegate = null;
        }

        @Override
        public ProtocolWriter writeName(String name) throws IOException {
            throw new NotSerializedToAValueException();
        }

        @Override
        public ProtocolWriter writeValue(Object value) throws IOException {
            delegate.writeValue(value);
            return this;
        }

        @Override
        public ProtocolWriter writeValue(String value) throws IOException {
            delegate.writeValue(value);
            return this;
        }

        @Override
        public ProtocolWriter writeValue(double value) throws IOException {
            delegate.writeValue(value);
            return this;
        }

        @Override
        public ProtocolWriter writeValue(float value) throws IOException {
            delegate.writeValue(value);
            return this;
        }

        @Override
        public ProtocolWriter writeValue(long value) throws IOException {
            delegate.writeValue(value);
            return this;
        }

        @Override
        public ProtocolWriter writeValue(boolean value) throws IOException {
            delegate.writeValue(value);
            return this;
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
