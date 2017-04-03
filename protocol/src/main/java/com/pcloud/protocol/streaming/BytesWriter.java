/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Georgi Neykov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.pcloud.protocol.streaming;

import com.pcloud.protocol.DataSource;
import okio.Buffer;
import okio.BufferedSink;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ProtocolException;
import java.nio.charset.Charset;

import static com.pcloud.IOUtils.closeQuietly;

public class BytesWriter implements ProtocolWriter {

    private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");
    private static final int REQUEST_PARAM_TYPE_STRING = 0;
    private static final int REQUEST_PARAM_TYPE_NUMBER = 1;
    private static final int REQUEST_PARAM_TYPE_BOOLEAN = 2;
    private static final int REQUEST_PARAM_COUNT_LIMIT = 255;
    private static final int REQUEST_PARAM_NAME_LENGTH_LIMIT = 63;
    private static final int REQUEST_SIZE_LIMIT_BYTES = 65535;
    private static final int REQUEST_BINARY_DATA_FLAG_POSITION = 7;

    private BufferedSink sink;
    private Buffer paramsBuffer;
    private DataSource dataSource;
    private long dataSourceLength;

    private String methodName;
    private boolean requestStarted;
    private boolean hasData;
    private boolean methodAdded;
    private boolean dataAdded;
    private int parameterCount;

    public BytesWriter(BufferedSink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("'sink' argument cannot be null.");
        }

        this.sink = sink;
        this.paramsBuffer = new Buffer();
    }

    @Override
    public ProtocolWriter beginRequest() throws IOException {
        if (requestStarted) {
            throw new IllegalStateException("Call endRequest() before beginning a new method.");
        }
        requestStarted = true;
        return this;
    }

    @Override
    public ProtocolWriter method(String name) throws IOException {
        if (name == null) {
            throw new IllegalArgumentException("'name' argument cannot be null.");
        }
        if (!requestStarted) {
            throw new IllegalStateException("Call beginRequest() or beginDataRequest() before calling this method.");
        }
        if (methodAdded) {
            throw new IllegalStateException("method() already called for the started request.");
        }
        methodName = name;
        methodAdded = true;
        return this;
    }

    @Override
    public ProtocolWriter endRequest() throws IOException {
        byte[] bytes = methodName.getBytes(PROTOCOL_CHARSET);

        int methodNameLength = bytes.length;
        if (methodNameLength > 127) {
            throw new ProtocolException("Method name cannot be longer than 127 characters.");
        }

        if (hasData) {
            methodNameLength = methodNameLength | (1 << REQUEST_BINARY_DATA_FLAG_POSITION);
        }

        sink.writeByte(methodNameLength);
        sink.write(bytes);

        if (hasData) {
            sink.writeLongLe(dataSourceLength);
        }

        final long parametersSize = paramsBuffer.size();
        if (parametersSize > REQUEST_SIZE_LIMIT_BYTES - 1) {
            throw new ProtocolException("The request is too big, max allowed size is 65535 bytes, current is " + parametersSize);
        }

        sink.writeByte(parameterCount);
        sink.writeShortLe((int) parametersSize + 1);
        sink.write(paramsBuffer, parametersSize);
        if (hasData) {
            dataSource.writeTo(sink);
        }

        dataSource = null;
        requestStarted = hasData = methodAdded = dataAdded = false;
        parameterCount = 0;
        dataSourceLength = 0;
        return this;
    }

    @Override
    public ProtocolWriter data(DataSource source) throws IOException {
        if (source == null) {
            throw new IllegalArgumentException("'source' argument cannot be null.");
        }

        final long sizeBytes = source.contentLength();
        if (sizeBytes < 0L) {
            throw new IllegalArgumentException("'sizeBytes' argument cannot be a negative number.");
        }

        if (!requestStarted) {
            throw new IllegalStateException("Call beginDataRequest() before calling this method.");
        }
        if (!hasData) {
            throw new IllegalStateException("beginDataRequest() must be used when writing a data request.");
        }
        if (!methodAdded) {
            throw new IllegalStateException("Call method() before adding data to request.");
        }
        if (dataAdded) {
            throw new IllegalStateException("data() already called for current request.");
        }

        dataSource = source;
        dataSourceLength = sizeBytes;
        dataAdded = true;
        return this;
    }

    @Override
    public ProtocolWriter parameter(String name, Object value) throws IOException {
        if (value != null) {
            Type valueType = value.getClass();
            if (valueType == String.class) {
                parameter(name, (String) value);
            } else if (valueType == Long.class) {
                parameter(name, (long) value);
            } else if (valueType == Integer.class) {
                parameter(name, (int) value);
            } else if (valueType == Short.class) {
                parameter(name, (short) value);
            } else if (valueType == Byte.class) {
                parameter(name, (byte) value);
            } else if (valueType == Boolean.class) {
                parameter(name, (boolean) value);
            } else {
                throw new ProtocolException("Cannot serialize value of type '" + valueType + "'.");
            }
        }
        return this;
    }

    @Override
    public ProtocolWriter parameter(String name, String value) throws IOException {
        if (value != null) {
            writeParameterTypeAndName(name, REQUEST_PARAM_TYPE_STRING);
            byte[] bytes = value.getBytes(PROTOCOL_CHARSET);
            paramsBuffer.writeIntLe(bytes.length);
            paramsBuffer.write(bytes);
        }

        return this;
    }

    @Override
    public ProtocolWriter parameter(String name, long value) throws IOException {
        if (value < 0) {
            parameter(name, String.valueOf(value));
        } else {
            writeParameterTypeAndName(name, REQUEST_PARAM_TYPE_NUMBER);
            paramsBuffer.writeLongLe(value);
        }

        return this;
    }

    @Override
    public ProtocolWriter parameter(String name, boolean value) throws IOException {
        writeParameterTypeAndName(name, REQUEST_PARAM_TYPE_BOOLEAN);
        paramsBuffer.writeByte(value ? 1 : 0);
        return this;
    }

    private void writeParameterTypeAndName(String name, int type) throws IOException {
        if (parameterCount >= REQUEST_PARAM_COUNT_LIMIT) {
            throw new ProtocolException("Request parameter count limit reached.");
        }
        byte[] bytes = name.getBytes(PROTOCOL_CHARSET);
        int parameterNameLength = bytes.length;
        if (parameterNameLength > REQUEST_PARAM_NAME_LENGTH_LIMIT) {
            throw new ProtocolException("Parameter '" + name + "' is too long, should be no more than 63 characters.");
        }

        int encodedParamData = parameterNameLength | (type << 6);
        paramsBuffer.writeByte(encodedParamData);
        paramsBuffer.write(bytes);
        parameterCount++;
    }

    @Override
    public void close() {
        closeQuietly(sink);
        closeQuietly(paramsBuffer);
        dataSource = null;
    }

    @Override
    public void flush() throws IOException {
        sink.flush();
    }
}
