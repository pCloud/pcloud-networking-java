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

package com.pcloud.networking.serialization;

import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;

import java.io.IOException;
/**
 * An interface for converting between objects and their binary protocol representations.
 * <p>
 * Another feature is the ability to convert between objects and their representation in the form of key-value pairs.
 *
 * @param <T> The type that this adapter can convert
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class TypeAdapter<T> {

    /**
     * Deserializes binary data from a {@linkplain ProtocolReader} to a concrete java data type
     *
     * @param reader A {@linkplain ProtocolReader} to read the data
     * @return A concrete java implementation of the data type this adapter works with
     * @throws IOException on failed IO operations
     */
    public abstract T deserialize(ProtocolReader reader) throws IOException;

    /**
     * Serializes data from an object and writes it to a {@linkplain ProtocolWriter}
     *
     * @param writer A {@linkplain ProtocolWriter} to write the data
     * @param value  A concrete java implementation of the data type to be serialized
     * @throws IOException on failed IO operations
     */
    public abstract void serialize(ProtocolWriter writer, T value) throws IOException;
}
