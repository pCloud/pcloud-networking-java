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

import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.protocol.ProtocolReader;

import java.io.IOException;
import java.util.Collection;

abstract class CollectionTypeAdapter<T extends Collection<E>, E> extends TypeAdapter<T> {

    private TypeAdapter<E> elementAdapter;

    CollectionTypeAdapter(TypeAdapter<E> elementAdapter) {
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
        if (value != null && value.size() > 0) {
            StringJoiner joiner = new StringJoiner(",");
            for (E item : value) {
                if (item != null) {
                    joiner.join(item.toString());
                }
            }

            writer.writeValue(joiner.toString());
        }
    }

    protected abstract T instantiateCollection();
}
