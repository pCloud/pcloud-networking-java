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

package com.pcloud.networking.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An utility object able to read data
 */
public class ValueReader {

    /**
     * Read data with a {@linkplain ProtocolReader} and extract all the data from the readers source
     * <p>
     *
     * @param reader a reader which should read from its source to extract the data
     * @return an {@linkplain Object} which is actually one of the {@linkplain TypeToken} types, a {@linkplain Map} or an array of {@linkplain Object}
     * @throws IOException on failed IO operations
     */
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
                throw new IOException("Unexpected token '" + token.toString() + "'.");
        }
    }

    /**
     * Read the data from a readers source and extract it to a {@linkplain Map}
     * <p>
     *
     * @param reader the {@linkplain ProtocolReader} from which the data can be extracted
     * @return {@linkplain Map} which contains all the data as key - value pairs
     * @throws IOException on failed IO operations
     */
    public Map<String, ?> readObject(ProtocolReader reader) throws IOException {
        reader.beginObject();
        Map<String, ? super Object> values = new HashMap<>();
        while (reader.hasNext()) {
            values.put(reader.readString(), read(reader));
        }
        reader.endObject();
        return values;
    }

    /**
     * Read all the data from readers source and extract it to an {@linkplain Object} array
     * <p>
     *
     * @param reader {@linkplain ProtocolReader} from the source of which data will be extracted
     * @return An {@linkplain Object} array with all the data
     * @throws IOException on failed IO operations
     */
    public Object[] readArray(ProtocolReader reader) throws IOException {
        return readList(reader).toArray();
    }

    /**
     * Read all the data from a readers source and extract it as a {@linkplain List}
     * <p>
     *
     * @param reader {@linkplain ProtocolReader} from the source of which data will be extracted
     * @return a {@linkplain List} containing all the data from the readers source
     * @throws IOException on failed IO operations
     */
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
