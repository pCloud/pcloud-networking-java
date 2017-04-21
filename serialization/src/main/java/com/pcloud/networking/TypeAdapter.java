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
import com.pcloud.protocol.streaming.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * An interface for converting between objects and their binary protocol representations.
 * <p>
 * Another feature is the ability to convert between objects and their representation in the form of key-value pairs.
 * @param <T> The type that this adapter can convert
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class TypeAdapter<T> {

    public abstract T deserialize(ProtocolReader reader) throws IOException;

    public abstract void serialize(ProtocolWriter writer, T value) throws IOException;

    public Map<String, ?> toProtocolValue(T value) {
        throw new UnsupportedOperationException();
    }

    public T fromProtocolValue(Map<String, ?> value){
        throw new UnsupportedOperationException();
    }
}
