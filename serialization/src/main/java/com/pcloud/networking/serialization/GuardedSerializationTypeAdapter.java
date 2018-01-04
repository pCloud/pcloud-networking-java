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
 * А {@linkplain TypeAdapter} implementation that enforces a "one-parameter-from-object" serialization rule
 * <p>
 * This implementation is useful for wrapping other {@linkplain TypeAdapter} instances and
 * ensuring that the objects supplied to the adapter will be serialized to no more than a single key-value parameter.
 *
 * @param <T> The type that this adapter can convert
 */
public class GuardedSerializationTypeAdapter<T> extends TypeAdapter<T> {

    private static final int GUARD_POOL_SIZE = 15;

    private TypeAdapter<T> delegate;
    private ObjectPool<GuardedWriter> guardedWriterObjectPool = new ObjectPool<>(GUARD_POOL_SIZE);

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
        GuardedWriter guardedWriter = guardedWriterObjectPool.acquire();
        if (guardedWriter == null) {
            guardedWriter = new GuardedWriter();
        }
        guardedWriter.guard(writer);
        try {
            delegate.serialize(guardedWriter, value);
        } finally {
            guardedWriter.reset();
            guardedWriterObjectPool.recycle(guardedWriter);
        }
    }

    private static class ObjectPool<T> {
        private final Object[] pool;
        private int mPoolSize;

        ObjectPool(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("The max pool size must be > 0");
            }
            pool = new Object[maxPoolSize];
        }

        @SuppressWarnings("unchecked")
        public T acquire() {
            synchronized (pool) {
                if (mPoolSize > 0) {
                    final int lastPooledIndex = mPoolSize - 1;
                    T instance = (T) pool[lastPooledIndex];
                    pool[lastPooledIndex] = null;
                    mPoolSize--;
                    return instance;
                }
                return null;
            }
        }

        public boolean recycle(T instance) {
            synchronized (pool) {
                if (mPoolSize < pool.length) {
                    pool[mPoolSize] = instance;
                    mPoolSize++;
                    return true;
                }
                return false;
            }
        }

    }

    private static class GuardedWriter implements ProtocolWriter {

        private int parametersAdded;
        private ProtocolWriter delegate;

        GuardedWriter() {
            this.parametersAdded = 0;
        }

        ProtocolWriter guard(ProtocolWriter writer) {
            delegate = writer;
            parametersAdded = 0;
            return this;
        }

        void reset() {
            delegate = null;
        }

        @Override
        public ProtocolWriter writeName(String name) throws IOException {
            if (parametersAdded != 0) {
                throw new SerializationException("Object must serialize to a single parameter.");
            }
            delegate.writeName(name);
            parametersAdded++;
            return this;
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
