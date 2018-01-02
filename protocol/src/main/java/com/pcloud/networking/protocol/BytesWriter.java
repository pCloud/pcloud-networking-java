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

package com.pcloud.networking.protocol;

import okio.Buffer;
import okio.BufferedSink;
import okio.Utf8;

import java.io.IOException;
import java.lang.reflect.Type;

import static com.pcloud.utils.IOUtils.closeQuietly;

/**
 * Writes bytes into a sink
 * <p>
 * An implementation of a {@linkplain ProtocolRequestWriter} which is able to write bytes into a {@linkplain BufferedSink}
 * <p>
 * Generally used to write a network request
 *
 * @see ProtocolWriter
 * @see ProtocolRequestWriter
 * @see BufferedSink
 */
public class BytesWriter implements ProtocolRequestWriter {

    // Request parameter types
    private static final int REQUEST_PARAM_TYPE_STRING = 0;
    private static final int REQUEST_PARAM_TYPE_NUMBER = 1;
    private static final int REQUEST_PARAM_TYPE_BOOLEAN = 2;

    private static final int REQUEST_PARAM_COUNT_LIMIT = 255;
    private static final int REQUEST_PARAM_NAME_LENGTH_LIMIT = 63;
    private static final int REQUEST_SIZE_LIMIT_BYTES = 65535;
    private static final int REQUEST_BINARY_DATA_FLAG_POSITION = 7;
    private static final int MAX_METHOD_LENGTH = 127;
    private static final int BITWISE_SHIFT_SIX = 6;
    public static final int DATA_LENGTH_SIZE = 8;
    public static final int METHOD_NAME_LENGTH_SIZE = 2;

    private final BufferedSink sink;
    private final Buffer paramsBuffer;
    private DataSource dataSource;

    private String methodName;
    private boolean requestStarted;
    private int parameterCount;

    private String nextValueName;

    /**
     * Create a {@linkplain BytesReader} instance
     * <p>
     *
     * @param sink a {@linkplain BufferedSink} to house the bytes
     * @throws IllegalArgumentException on a null {@linkplain BufferedSink} argument
     */
    public BytesWriter(BufferedSink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("'sink' argument cannot be null.");
        }

        this.sink = sink;
        this.paramsBuffer = new Buffer();
    }

    @Override
    public ProtocolRequestWriter beginRequest() throws IOException {
        if (requestStarted) {
            throw new IllegalStateException("beginRequest() has been already called.");
        }
        requestStarted = true;
        return this;
    }

    @Override
    public ProtocolRequestWriter writeMethodName(String name) throws IOException {
        checkRequestStarted();
        checkWriteValueFinished();
        if (name == null) {
            throw new IllegalArgumentException("'name' argument cannot be null.");
        }
        if (methodName != null) {
            throw new IllegalStateException("method() already called for the started request.");
        }
        methodName = name;
        return this;
    }


    @Override
    public ProtocolRequestWriter endRequest() throws IOException {
        checkRequestStarted();
        checkWriteValueFinished();

        if (methodName == null) {
            throw new IllegalArgumentException("Cannot end request, " +
                    "writeMethodName() has not been called.");
        }

        int methodNameLength = (int) Utf8.size(methodName);
        if (methodNameLength > MAX_METHOD_LENGTH) {
            throw new SerializationException(
                    "Invalid method name '%s', " +
                            "value cannot be larger than 127 bytes in UTF-8 encoding.", methodName);
        }

        final long dataSourceLength = dataSource != null ? dataSource.contentLength() : 0;
        if (dataSourceLength < 0L) {
            throw new SerializationException(
                    "Unknown or invalid DataSource content length '%d'.", dataSourceLength);
        }

        boolean hasData = dataSourceLength > 0;

        final long requestSize =
                (hasData ? DATA_LENGTH_SIZE : 0) + // Data length, if any (8 bytes)
                        1 + // Method name length + data flag (1 byte)
                        1 + // + Parameter count (1 byte)
                        methodNameLength +
                        +paramsBuffer.size();
        if (requestSize > REQUEST_SIZE_LIMIT_BYTES) {
            throw new SerializationException("The maximum allowed request size is 65535 bytes," +
                    " current is " + requestSize + " bytes.");
        }

        if (hasData) {
            methodNameLength = methodNameLength | (1 << REQUEST_BINARY_DATA_FLAG_POSITION);
        }

        sink.writeShortLe((int) requestSize); // Request size, 2 bytes
        sink.writeByte(methodNameLength);
        if (hasData) {
            sink.writeLongLe(dataSourceLength); // Size of data after response, 8 bytes
        }
        sink.writeUtf8(methodName);
        sink.writeByte(parameterCount);
        sink.write(this.paramsBuffer, paramsBuffer.size());
        if (hasData) {
            dataSource.writeTo(sink);
        }

        sink.emit();
        dataSource = null;
        methodName = null;
        requestStarted = false;
        parameterCount = 0;
        return this;
    }

    @Override
    public ProtocolRequestWriter writeData(DataSource source) throws IOException {
        checkRequestStarted();
        checkWriteValueFinished();
        if (dataSource != null) {
            throw new IllegalStateException("data() already called for current request.");
        }

        if (source == null) {
            throw new IllegalArgumentException("DataSource argument cannot be null.");
        }

        dataSource = source;
        return this;
    }

    @Override
    public ProtocolRequestWriter writeName(String name) throws IOException {
        checkRequestStarted();
        checkWriteValueFinished();

        if (name == null) {
            throw new IllegalArgumentException("Name parameter cannot be null.");
        }

        if (nextValueName != null) {
            throw new IllegalStateException("A previous writeName() was not matched with a writeValue() call.");
        }

        if (parameterCount >= REQUEST_PARAM_COUNT_LIMIT) {
            throw new SerializationException("Request parameter count limit reached.");
        }

        nextValueName = name;
        parameterCount++;
        return this;
    }

    private void writeNextValueTypeAndName(int type) throws SerializationException {
        int parameterNameLength = (int) Utf8.size(nextValueName);
        if (parameterNameLength > REQUEST_PARAM_NAME_LENGTH_LIMIT) {
            throw new SerializationException("Parameter name '%s' is too long.", nextValueName);
        }

        int encodedParamData = parameterNameLength | (type << BITWISE_SHIFT_SIX);
        paramsBuffer.writeByte(encodedParamData);
        paramsBuffer.writeUtf8(nextValueName);
        nextValueName = null;
    }

    @Override
    public ProtocolRequestWriter writeValue(Object value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Value argument cannot be null");
        }

        final Type valueType = value.getClass();
        if (valueType == String.class) {
            writeValue((String) value);
        } else if (valueType == Long.class) {
            writeValue((long) value);
        } else if (valueType == Integer.class) {
            writeValue((int) value);
        } else if (valueType == Float.class) {
            writeValue((float) value);
        } else if (valueType == Double.class) {
            writeValue((double) value);
        } else if (valueType == Short.class) {
            writeValue((short) value);
        } else if (valueType == Byte.class) {
            writeValue((byte) value);
        } else if (valueType == Boolean.class) {
            writeValue((boolean) value);
        } else {
            throw new IllegalArgumentException("Cannot serialize value of type '" + valueType + "'.");
        }
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(String value) throws IOException {
        checkWriteNameCalled();
        if (value == null) {
            throw new IllegalArgumentException("Value argument cannot be null");
        }
        writeNextValueTypeAndName(REQUEST_PARAM_TYPE_STRING);
        writeString(value);

        return this;
    }

    private void writeString(String value) {
        paramsBuffer.writeIntLe((int) Utf8.size(value));
        paramsBuffer.writeUtf8(value);
    }

    @Override
    public ProtocolRequestWriter writeValue(long value) throws IOException {
        checkWriteNameCalled();
        if (value < 0) {
            writeNextValueTypeAndName(REQUEST_PARAM_TYPE_STRING);
            writeString(String.valueOf(value));
        } else {
            writeNextValueTypeAndName(REQUEST_PARAM_TYPE_NUMBER);
            paramsBuffer.writeLongLe(value);
        }

        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(boolean value) throws IOException {
        checkWriteNameCalled();
        writeNextValueTypeAndName(REQUEST_PARAM_TYPE_BOOLEAN);
        paramsBuffer.writeByte(value ? 1 : 0);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(double value) throws IOException {
        checkWriteNameCalled();
        writeNextValueTypeAndName(REQUEST_PARAM_TYPE_STRING);
        writeString(String.valueOf(value));
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(float value) throws IOException {
        checkWriteNameCalled();
        writeNextValueTypeAndName(REQUEST_PARAM_TYPE_STRING);
        writeString(String.valueOf(value));
        return this;
    }

    /**
     * Close the {@linkplain BufferedSink}
     */
    @Override
    public void close() {
        closeQuietly(sink);
        closeQuietly(paramsBuffer);
        dataSource = null;
    }

    private void checkRequestStarted() {
        if (!requestStarted) {
            throw new IllegalStateException("Call beginRequest() before calling this method.");
        }
    }

    private void checkWriteValueFinished() {
        if (nextValueName != null) {
            throw new IllegalStateException("Expected a call to one of the writeValue() methods.");
        }
    }

    private void checkWriteNameCalled() {
        if (nextValueName == null) {
            throw new IllegalStateException("Call writeName() before calling this method.");
        }
    }
}
