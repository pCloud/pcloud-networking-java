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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

class ArrayTypeAdapter extends TypeAdapter<Object> {

    private final ObjectPool<StringJoinerProtocolWriter> joinerWriterPool = new ObjectPool<>(5);

    private Class<?> elementClass;
    private TypeAdapter elementAdapter;

    ArrayTypeAdapter(Class<?> elementClass, TypeAdapter elementAdapter) {
        this.elementClass = elementClass;
        this.elementAdapter = elementAdapter;
    }

    @Override
    public Object deserialize(ProtocolReader reader) throws IOException {
        List<Object> elements = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            elements.add(elementAdapter.deserialize(reader));
        }
        reader.endArray();
        Object array = Array.newInstance(elementClass, elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Array.set(array, i, elements.get(i));
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(ProtocolWriter writer, Object value) throws IOException {
        if (value != null) {
            final int arrayLength = Array.getLength(value);
            if (arrayLength > 0) {
                StringJoinerProtocolWriter joinerWriter = joinerWriterPool.acquire();
                if (joinerWriter == null) {
                    joinerWriter = new StringJoinerProtocolWriter(",");
                }
                try {
                    for (int i = 0; i < arrayLength; i++) {
                        elementAdapter.serialize(joinerWriter, Array.get(value, i));
                    }
                    writer.writeValue(joinerWriter.result());
                } finally {
                    joinerWriter.reset();
                    joinerWriterPool.recycle(joinerWriter);
                }
            } else {
                writer.writeValue("");
            }
        }
    }

    @Override
    public String toString() {
        return "TypeAdapter[" + elementClass.getName() + "[]]";
    }

}
