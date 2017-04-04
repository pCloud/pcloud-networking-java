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

import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.util.Map;

public class ValueWriter {

    public void writeAll(ProtocolWriter writer, Map<String, ?> values) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("ProtocolWriter argument cannot be null.");
        }

        if (values == null) {
            throw new IllegalArgumentException("Values argument cannot be null.");
        }

        for (Map.Entry<String, ?> entry : values.entrySet()){
            writer.parameter(entry.getKey(), entry.getValue());
        }
    }
}