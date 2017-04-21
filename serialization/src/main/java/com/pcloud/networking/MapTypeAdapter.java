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

package com.pcloud.networking;

import com.pcloud.protocol.streaming.ProtocolReader;
import com.pcloud.protocol.streaming.ProtocolWriter;
import com.pcloud.protocol.streaming.SerializationException;
import com.pcloud.protocol.streaming.UnserializableTypeException;

import java.io.IOException;
import java.util.Map;

abstract class MapTypeAdapter<K,V> extends TypeAdapter<Map<K,V>>{

    private TypeAdapter<K> keyAdapter;
    private TypeAdapter<V> valueAdapter;

    MapTypeAdapter(TypeAdapter<K> keyAdapter, TypeAdapter<V> valueAdapter) {
        this.keyAdapter = keyAdapter;
        this.valueAdapter = valueAdapter;
    }

    @Override
    public Map<K,V> deserialize(ProtocolReader reader) throws IOException {
        Map<K,V> container = instantiateCollection();
        reader.beginObject();
        while (reader.hasNext()) {
            container.put(keyAdapter.deserialize(reader), valueAdapter.deserialize(reader));
        }
        reader.endObject();
        return container;
    }

    @Override
    public void serialize(ProtocolWriter writer, Map<K,V> value) throws IOException {
        throw new UnserializableTypeException(value.getClass());
    }

    protected abstract Map<K,V> instantiateCollection();
}
