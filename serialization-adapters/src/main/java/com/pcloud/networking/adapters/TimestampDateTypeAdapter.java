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

package com.pcloud.networking.adapters;

import com.pcloud.networking.TypeAdapter;
import com.pcloud.protocol.streaming.ProtocolReader;
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.util.Date;

public class TimestampDateTypeAdapter extends TypeAdapter<Date> {
    private static final long DATE_MULTIPLIER = 1000L;
    @Override
    public Date deserialize(ProtocolReader reader) throws IOException {
        return new Date(reader.readNumber() * DATE_MULTIPLIER);
    }

    @Override
    public void serialize(ProtocolWriter writer, Date value) throws IOException {
        writer.writeValue(value.getTime() / DATE_MULTIPLIER);
    }
}
