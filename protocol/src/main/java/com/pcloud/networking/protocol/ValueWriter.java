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
import java.util.Map;

/**
 * A utility class designed to write data from a source to a sink
 */
public class ValueWriter {

    /**
     * Write all the data from a {@linkplain ProtocolWriter} to a {@linkplain Map}
     * <p>
     *
     * @param writer a {@linkplain ProtocolWriter} to write the data
     * @param values a {@linkplain Map} to take the data and act as a data sink
     * @throws IOException on failed IO operations
     * @throws IllegalArgumentException on null arguments
     */
    public void writeAll(ProtocolWriter writer, Map<String, ?> values) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("ProtocolWriter argument cannot be null.");
        }

        if (values == null) {
            throw new IllegalArgumentException("Values argument cannot be null.");
        }

        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                writer.writeName(name).writeValue(value);
            }
        }
    }
}
