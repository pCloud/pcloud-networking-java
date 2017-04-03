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

package com.pcloud.protocol;

import com.pcloud.protocol.streaming.ProtocolReader;
import com.pcloud.protocol.streaming.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueReader {

    public Object read(ProtocolReader reader) throws IOException {
        TypeToken token = reader.peek();
        switch (token) {
            case NUMBER:
                return reader.readNumber();
            case STRING:
                return reader.readString();
            case BOOLEAN:
                return reader.readBoolean();
            case BEGIN_ARRAY:
                return readArray(reader);
            case BEGIN_OBJECT:
                return readObject(reader);
            default:
                throw new IOException("Unexpected token '"+ token.toString()+"'.");
        }
    }

    public Map<String, ?> readObject(ProtocolReader reader) throws IOException {
        reader.beginObject();
        Map<String,? super Object> values = new HashMap<>();
        while (reader.hasNext()){
            values.put(reader.readString(), read(reader));
        }
        reader.endObject();
        return values;
    }

    public Object[] readArray(ProtocolReader reader) throws IOException {
        return readList(reader).toArray();
    }

    public List<?> readList(ProtocolReader reader) throws IOException {
        reader.beginArray();
        List<? super Object> list = new ArrayList<>();
        while (reader.hasNext()) {
            list.add(read(reader));
        }
        reader.endArray();
        return list;
    }
}
