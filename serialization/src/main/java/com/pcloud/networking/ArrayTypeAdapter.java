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
import java.lang.reflect.Array;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;

class ArrayTypeAdapter extends TypeAdapter<Object>  {

    private Class<?> elementClass;
    private TypeAdapter<Object> elementAdapter;

    ArrayTypeAdapter(Class<?> elementClass, TypeAdapter<Object> elementAdapter) {
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

    @Override
    public void serialize(ProtocolWriter writer, Object value) throws IOException {
        throw new ProtocolException("Serializing arrays is not supported.");
    }

    @Override public String toString() {
        return "TypeAdapter[" + elementClass.getName() + "[]]";
    }
}
