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

import com.pcloud.networking.protocol.SerializationException;
import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Class<T> enumType;
    private final Map<String, T> nameToConstantMap;
    private final Map<T, String> constantToNameMap;

    EnumTypeAdapter(Class<T> enumType) {
        this.enumType = enumType;
        try {
            T[] constants = enumType.getEnumConstants();
            this.nameToConstantMap = new HashMap<>(constants.length);
            this.constantToNameMap = new HashMap<>(constants.length);
            for (int i = 0; i < constants.length; i++) {
                T constant = constants[i];
                ParameterValue annotation = enumType.getField(constant.name()).getAnnotation(ParameterValue.class);
                String name = annotation != null && !annotation.value().equals(ParameterValue.DEFAULT_NAME) ?
                                      annotation.value() : constant.name();
                nameToConstantMap.put(name, constant);
                constantToNameMap.put(constant, name);
            }
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field in " + enumType.getName(), e);
        }
    }


    @Override
    public T deserialize(ProtocolReader reader) throws IOException {
        String name = reader.readString();
        T enumConstant = nameToConstantMap.get(name);
        if (enumConstant != null) {
            return enumConstant;
        } else {
            throw new SerializationException("Cannot deserialize '" +
                                                     enumType.getName() +
                                                     "':\nExpected one of " +
                                                     nameToConstantMap.keySet() +
                                                     " but was '" +
                                                     name +
                                                     "'.");
        }
    }

    @Override
    public void serialize(ProtocolWriter writer, T value) throws IOException {
        if (value != null) {
            String name = constantToNameMap.get(value);
            writer.writeValue(name);
        }
    }

    @Override
    public String toString() {
        return "TypeAdapter[" + enumType.getName() + "]";
    }
}
