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

package com.pcloud.networking.serialization.adapters;

import com.pcloud.networking.serialization.TypeAdapter;
import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;

import java.io.IOException;
import java.util.Date;

/**
 * Adapter that converts between Timestamp and {@linkplain Date}
 */
public class TimestampDateTypeAdapter extends TypeAdapter<Date> {

    private static final long DATE_MULTIPLIER = 1000L;

    /**
     * Deserializes binary data from a {@linkplain ProtocolReader} into a {@linkplain Date}
     *
     * @param reader A {@linkplain ProtocolReader} to read the data
     * @return A {@linkplain Date} object deserialized from the binary data
     * @throws IOException on failed IO operations
     */
    @Override
    public Date deserialize(ProtocolReader reader) throws IOException {
        return new Date(reader.readNumber() * DATE_MULTIPLIER);
    }

    /**
     * Serializes a {@linkplain Date} object and writes the binary data into a {@linkplain ProtocolWriter}
     *
     * @param writer A {@linkplain ProtocolWriter} to write the data
     * @param value  A concrete java implementation of the data type to be serialized
     * @throws IOException on failed IO operations
     */
    @Override
    public void serialize(ProtocolWriter writer, Date value) throws IOException {
        writer.writeValue(value.getTime() / DATE_MULTIPLIER);
    }
}
