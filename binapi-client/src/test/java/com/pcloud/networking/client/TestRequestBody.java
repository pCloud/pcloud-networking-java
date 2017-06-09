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

package com.pcloud.networking.client;

import com.pcloud.networking.protocol.ValueWriter;
import com.pcloud.networking.protocol.BytesWriter;
import com.pcloud.networking.protocol.ProtocolRequestWriter;
import com.pcloud.networking.protocol.ProtocolWriter;
import okio.Buffer;
import okio.ByteString;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class TestRequestBody extends RequestBody {

    private Map<String,Object> values;

    public static Builder create(){
        return new Builder(new TreeMap<String, Object>());
    }

    private TestRequestBody(Builder builder) {
        this.values = builder.values;
    }

    @Override
    public void writeTo(ProtocolWriter writer) throws IOException {
        new ValueWriter().writeAll(writer, values);
    }

    public ByteString bytes() throws IOException {
        Buffer buffer = new Buffer();
        ProtocolRequestWriter writer = new BytesWriter(buffer);
        try {
            this.writeTo(writer);
            return buffer.readByteString();
        } finally {
            buffer.close();
            writer.close();
        }
    }

    public Builder newBuilder(){
        return new Builder(values);
    }

    public static class Builder {

        private Map<String, Object> values;

        private Builder(Map<String, Object> values){
            this.values = values;
        }

        public Builder setValues(Map<String, ?> values) {
            this.values.putAll(values);
            return this;
        }

        public Builder setValue(String key, Object value) {
            this.values.put(key, value);
            return this;
        }

        public TestRequestBody build() {
            return new TestRequestBody(this);
        }
    }
}
