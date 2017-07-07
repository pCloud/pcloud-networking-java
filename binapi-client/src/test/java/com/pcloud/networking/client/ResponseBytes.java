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

import com.pcloud.networking.protocol.ValueReader;
import com.pcloud.networking.protocol.BytesReader;
import com.pcloud.networking.protocol.ProtocolResponseReader;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;

import java.io.IOException;
import java.util.Map;

@SuppressWarnings("WeakerAccess,unused")
public class ResponseBytes {

    public static ResponseBytes empty(){
        return new ResponseBytes();
    }

    public static ResponseBytes from(Map<String, ?> values) {
        ResponseBytes bytesBuilder = new ResponseBytes();
        try {
            for (Map.Entry<String, ?> pair : values.entrySet()) {
                bytesBuilder.writeValue(pair.getKey(), pair);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bytesBuilder;
    }

    private ResponseBytes(ResponseBytes responseBytes) {
        this.valuesBuffer = responseBytes.valuesBuffer.clone();
    }

    public ResponseBytes() {
        this.valuesBuffer = new Buffer();
    }

    public interface ArrayWriter {
        ArrayWriter write(Object value) throws IOException;

        ArrayWriter write(String value) throws IOException;

        ArrayWriter write(long value) throws IOException;

        ArrayWriter write(boolean value) throws IOException;

        ResponseBytes endArray() throws IOException;
    }

    private Buffer valuesBuffer;

    private final ArrayWriter arrayWriter = new ArrayWriter() {
        @Override
        public ArrayWriter write(Object value) throws IOException {
            ResponseBytes.this.write(value);
            return this;
        }

        @Override
        public ArrayWriter write(String value) throws IOException {
            ResponseBytes.this.write(value);
            return this;
        }

        @Override
        public ArrayWriter write(long value) throws IOException {
            ResponseBytes.this.write(value);
            return this;
        }

        @Override
        public ArrayWriter write(boolean value) throws IOException {
            ResponseBytes.this.write(value);
            return this;
        }

        @Override
        public ResponseBytes endArray() throws IOException {
            return ResponseBytes.this.endArray();
        }
    };

    public ResponseBytes beginObject() throws IOException {
        valuesBuffer.writeByte(16);
        return this;
    }

    public ResponseBytes endObject() throws IOException {
        valuesBuffer.writeByte(0xFF);
        return this;
    }

    public ArrayWriter beginArray() throws IOException {
        valuesBuffer.writeByte(17);
        return arrayWriter;
    }

    public ResponseBytes endArray() throws IOException {
        return endObject();
    }

    public ResponseBytes writeValue(String key, String value) throws IOException {
        return write(key).write(value);
    }

    public ResponseBytes writeValue(String key, long value) throws IOException {
        return write(key).write(value);
    }

    public ResponseBytes writeValue(String key, boolean value) throws IOException {
        return write(key).write(value);
    }

    public ResponseBytes writeValue(String key, Object value) throws IOException {
        return write(key).write(value);
    }

    private ResponseBytes write(Object value) throws IOException {
        final Class valueType = value.getClass();
        if (valueType == String.class) {
            return write((String) value);
        } else if (valueType == Long.class || valueType == long.class) {
            return write((long) value);
        } else if (valueType == Integer.class || valueType == int.class) {
            return write((int) value);
        } else if (valueType == Short.class || valueType == short.class) {
            return write((short) value);
        } else if (valueType == Byte.class || valueType == byte.class) {
            return write((byte) value);
        } else if (valueType == Boolean.class || valueType == boolean.class) {
            return write((boolean) value);
        } else if (valueType.isArray()) {
            beginArray();
            for (Object o : (Object[]) value) {
                write(o);
            }
            endArray();
            return this;
        } else if (Map.class.isAssignableFrom(valueType)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            if (!map.isEmpty()) {
                beginObject();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    writeValue(entry.getKey(), entry.getValue());
                }
                endObject();
            } else {
                beginArray();
                endArray();
            }
            return this;
        } else {
            throw new IllegalArgumentException("Cannot serialize value of type '" + valueType + "'.");
        }
    }

    private ResponseBytes write(String value) throws IOException {
        valuesBuffer.writeByte(3);
        valuesBuffer.writeIntLe(value.length());
        valuesBuffer.writeUtf8(value);
        return this;
    }

    private ResponseBytes write(long value) throws IOException {
        valuesBuffer.writeByte(15);
        valuesBuffer.writeLongLe(value);
        return this;
    }

    private ResponseBytes write(boolean value) throws IOException {
        valuesBuffer.writeByte(value ? 19 : 18);
        return this;
    }

    public ResponseBytes clone(){
        return new ResponseBytes(this);
    }

    public ResponseBytes clear() {
        valuesBuffer.clear();
        return this;
    }

    public long responseLength() {
        return valuesBuffer.size() + 2L;
    }

    public void writeTo(BufferedSink sink) throws IOException {
        Buffer values = valuesBuffer.clone();

        // Write response size
        sink.writeIntLe((int) values.size() + 2);
        if (values.size() > 0) {
            sink.writeByte(16); // Begin object
            sink.writeAll(values);
        } else {
            sink.writeByte(17); // Begin array
        }
        sink.writeByte(0XFF); // End object

    }

    public Map<String, ?> toValues() throws IOException {
        Buffer buffer = new Buffer();
        writeTo(buffer);
        ProtocolResponseReader responseReader = new BytesReader(buffer);
        responseReader.beginResponse();
        return new ValueReader().readObject(responseReader);
    }

    public ByteString bytes() {
        Buffer buffer = new Buffer();
        try {
            writeTo(buffer);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return buffer.readByteString();
    }
}
