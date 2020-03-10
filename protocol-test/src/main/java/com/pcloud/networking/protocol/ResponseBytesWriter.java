/*
 * Copyright (c) 2017 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.networking.protocol;

import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;

import java.io.IOException;
import java.util.Map;

import static com.pcloud.networking.protocol.Protocol.*;

@SuppressWarnings("WeakerAccess,unused")
public class ResponseBytesWriter {

    public static ResponseBytesWriter empty() {
        return new ResponseBytesWriter();
    }

    public static ResponseBytesWriter from(Map<String, ?> values) {
        ResponseBytesWriter bytesBuilder = new ResponseBytesWriter();
        for (Map.Entry<String, ?> pair : values.entrySet()) {
            bytesBuilder.writeValue(pair.getKey(), pair);
        }
        return bytesBuilder;
    }

    private Buffer valuesBuffer;
    private ByteString data;
    private final IntStack scopeStack;

    private final ArrayWriter arrayWriter = new ArrayWriter() {
        @Override
        public ArrayWriter write(Object value) throws IOException {
            checkScope(ProtocolReader.SCOPE_ARRAY);
            ResponseBytesWriter.this.write(value);
            return this;
        }

        @Override
        public ArrayWriter write(String value) throws IOException {
            checkScope(ProtocolReader.SCOPE_ARRAY);
            ResponseBytesWriter.this.write(value);
            return this;
        }

        @Override
        public ArrayWriter write(long value) throws IOException {
            checkScope(ProtocolReader.SCOPE_ARRAY);
            ResponseBytesWriter.this.write(value);
            return this;
        }

        @Override
        public ArrayWriter write(boolean value) throws IOException {
            checkScope(ProtocolReader.SCOPE_ARRAY);
            ResponseBytesWriter.this.write(value);
            return this;
        }

        @Override
        public ResponseBytesWriter endArray() throws IOException {
            checkScope(ProtocolReader.SCOPE_ARRAY);
            return ResponseBytesWriter.this.endArray();
        }
    };

    private ResponseBytesWriter(ResponseBytesWriter responseBytesWriter) {
        this.valuesBuffer = responseBytesWriter.valuesBuffer.clone();
        this.data = responseBytesWriter.data;
        this.scopeStack = new IntStack(responseBytesWriter.scopeStack);
    }

    public ResponseBytesWriter() {
        this.valuesBuffer = new Buffer();
        this.scopeStack = new IntStack();
    }

    public interface ArrayWriter {
        ArrayWriter write(Object value) throws IOException;

        ArrayWriter write(String value) throws IOException;

        ArrayWriter write(long value) throws IOException;

        ArrayWriter write(boolean value) throws IOException;

        ResponseBytesWriter endArray() throws IOException;
    }

    private int currentScope() {
        return scopeStack.isEmpty() ? ProtocolReader.SCOPE_NONE : scopeStack.peek();
    }

    private void checkScope(int expected) {
        if (currentScope() != expected) {
            throw new IllegalStateException("Invalid scope");
        }
    }

    public ResponseBytesWriter beginObject() {
        switch (currentScope()) {
            case ProtocolReader.SCOPE_ARRAY:
            case ProtocolReader.SCOPE_NONE:
            case ProtocolReader.SCOPE_OBJECT:
                break;
            default:
                throw new IllegalStateException("Invalid scope.");
        }
        scopeStack.push(ProtocolReader.SCOPE_OBJECT);
        valuesBuffer.writeByte(TYPE_BEGIN_OBJECT);
        return this;
    }

    public ResponseBytesWriter endObject() {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        scopeStack.pop();
        valuesBuffer.writeByte(TYPE_END_ARRAY_OBJECT);
        return this;
    }

    public ArrayWriter beginArray() {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        scopeStack.push(ProtocolReader.SCOPE_ARRAY);
        valuesBuffer.writeByte(TYPE_BEGIN_ARRAY);
        return arrayWriter;
    }

    public ResponseBytesWriter endArray() {
        checkScope(ProtocolReader.SCOPE_ARRAY);
        scopeStack.pop();
        valuesBuffer.writeByte(TYPE_END_ARRAY_OBJECT);
        return this;
    }

    public ResponseBytesWriter writeValues(Map<String, ?> values) {
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            writeValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public ResponseBytesWriter writeValue(String key, String value) {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        return write(key).write(value);
    }

    public ResponseBytesWriter writeValue(String key, long value) {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        return write(key).write(value);
    }

    public ResponseBytesWriter writeValue(String key, boolean value) {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        return write(key).write(value);
    }

    public ResponseBytesWriter writeValue(String key, Object value) {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        return write(key).write(value);
    }

    public ResponseBytesWriter writeKey(String key) {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        return write(key);
    }

    public ResponseBytesWriter setData(ByteString data) {
        checkScope(ProtocolReader.SCOPE_OBJECT);
        if (this.data != null) {
            throw new IllegalStateException("setData() already called.");
        }
        this.data = data;
        return write("data").writeDataSize(data.size());
    }

    private ResponseBytesWriter write(Object value) {
        final Class<?> valueType = value.getClass();
        if (valueType == String.class) {
            return write((String) value);
        } else if (valueType == Long.class) {
            return write((long) value);
        } else if (valueType == Integer.class) {
            return write((int) value);
        } else if (valueType == Short.class) {
            return write((short) value);
        } else if (valueType == Byte.class) {
            return write((byte) value);
        } else if (valueType == Boolean.class) {
            return write((boolean) value);
        } else if (valueType == Float.class || valueType == Double.class) {
            return write(String.valueOf(value));
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

    private ResponseBytesWriter write(String value) {
        valuesBuffer.writeByte(TYPE_STRING_END);
        valuesBuffer.writeIntLe(value.length());
        valuesBuffer.writeUtf8(value);
        return this;
    }

    private ResponseBytesWriter write(long value) {
        valuesBuffer.writeByte(TYPE_NUMBER_END);
        valuesBuffer.writeLongLe(value);
        return this;
    }

    private ResponseBytesWriter write(boolean value) {
        valuesBuffer.writeByte(value ? TYPE_BOOLEAN_TRUE : TYPE_BOOLEAN_FALSE);
        return this;
    }

    private ResponseBytesWriter writeDataSize(long size) {
        valuesBuffer.writeByte(TYPE_DATA);
        valuesBuffer.writeLongLe(size);
        return this;
    }

    public ResponseBytesWriter clone() {
        return new ResponseBytesWriter(this);
    }

    public ResponseBytesWriter clear() {
        valuesBuffer.clear();
        return this;
    }

    public long responseLength() {
        return valuesBuffer.size() + 2L;
    }

    public void writeTo(BufferedSink sink) throws IOException {
        Buffer values = valuesBuffer.clone();

        // Write response size
        sink.writeIntLe((int) values.size());
        sink.writeAll(values);
        if (data != null) {
            sink.write(data);
        }
    }

    public Map<String, ?> toValues() {
        Buffer buffer = new Buffer();
        try {
            writeTo(buffer);
            ProtocolResponseReader responseReader = new BytesReader(buffer);
            responseReader.beginResponse();
            Map<String, ?> values = new ValueReader().readObject(responseReader);
            responseReader.endResponse();
            return values;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
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

    public ProtocolResponseReader createReader() {
        Buffer values = new Buffer();
        try {
            writeTo(values);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return new BytesReader(values);
    }
}
