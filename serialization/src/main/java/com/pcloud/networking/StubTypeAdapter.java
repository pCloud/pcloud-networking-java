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

/**
 * A stub {@linkplain TypeAdapter} implementation used in cases
 * where a class has a field of the same type, (tree structures, graphs, ...).
 * Used to replace the real adapter while it gets fully created.
 *
 * Idea taken from Moshi's com.squareup.moshi.Moshi.DefferedJsonAdapter
 */
class StubTypeAdapter<T> extends TypeAdapter<T> {

    Object cacheKey;
    private TypeAdapter<T> delegate;

    StubTypeAdapter(Object cacheKey) {
        this.cacheKey = cacheKey;
    }

    void setDelegate(TypeAdapter<T> delegate) {
        this.delegate = delegate;
        this.cacheKey = null;
    }

    @Override
    public T deserialize(ProtocolReader reader) throws IOException {
        if (delegate == null) {
            throw new IllegalStateException("Type adapter isn't set");
        }
        return delegate.deserialize(reader);
    }

    @Override
    public void serialize(ProtocolWriter writer, T value) throws IOException {
        if (delegate == null) {
            throw new IllegalStateException("Type adapter isn't set");
        }
        delegate.serialize(writer, value);
    }
}