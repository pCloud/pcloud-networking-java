/*
 * Copyright (C) 2017 pCloud AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.net.ProtocolException;
import java.util.Collection;

public abstract class CollectionTypeAdapter<T extends Collection<E>, E> extends TypeAdapter<T>{

    private TypeAdapter<E> elementAdapter;

    protected CollectionTypeAdapter(TypeAdapter<E> elementAdapter) {
        this.elementAdapter = elementAdapter;
    }

    @Override
    public T deserialize(ProtocolReader reader) throws IOException {
        T container = instantiateCollection();
        reader.beginArray();
        while (reader.hasNext()) {
            container.add(elementAdapter.deserialize(reader));
        }
        reader.endArray();
        return container;
    }

    @Override
    public void serialize(ProtocolWriter writer, T value) throws IOException {
        throw new ProtocolException("Serializing arrays is not supported.");
    }

    protected abstract T instantiateCollection();
}