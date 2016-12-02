/*
 * Copyright (c) 2016 Georgi Neykov
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

package com.pcloud;

import com.pcloud.Data;
import com.pcloud.Request;
import com.pcloud.Response;
import com.pcloud.ResponseData;
import com.pcloud.value.*;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class BinaryProtocolCodec {

    private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");
    private static final int REQUEST_PARAM_TYPE_STRING = 0;
    private static final int REQUEST_PARAM_TYPE_NUMBER = 1;
    private static final int REQUEST_PARAM_TYPE_BOOLEAN = 2;
    private static final int REQUEST_PARAM_COUNT_LIMIT = 255;
    private static final int REQUEST_PARAM_NAME_LENGTH_LIMIT = 63;
    private static final int REQUEST_SIZE_LIMIT_BYTES = 65535;
    private static final int REQUEST_BINARY_DATA_FLAG_POSITION = 7;

    public long writeRequest(BufferedSink sink, Request request) throws IOException {
        Data data = request.data();
        Buffer requestBuffer = new Buffer();
        final long binaryDataSize = data != null ? data.contentLength() : 0;
        boolean hasBinaryData = binaryDataSize > 0;
        writeMethodName(requestBuffer, request.methodName(), hasBinaryData);
        if (hasBinaryData) {
            requestBuffer.writeLongLe(binaryDataSize);
        }

        ObjectValue requestParameters = request.requestParameters();
        if (requestParameters != null) {
            Set<Map.Entry<String, Value>> parameters = requestParameters.propertySet();
            int parameterCount = parameters.size();
            if (parameterCount > REQUEST_PARAM_COUNT_LIMIT) {
                throw new ProtocolException("Request parameter count exceed, max allowed is 255, current is " + parameterCount);
            }
            requestBuffer.writeByte(parameterCount);

            String parameterName;
            Value value;
            for (Map.Entry<String, Value> parameter : parameters) {
                value = parameter.getValue();
                parameterName = parameter.getKey();
                if (value.isString()) {
                    writeStringParameter(requestBuffer, parameterName, value.asString());
                } else if (value.isNumber()) {
                    writeNumberParameter(requestBuffer, parameterName, value.asNumber());
                } else if (value.isBoolean()) {
                    writeBooleanParameter(requestBuffer, parameterName, value.asBoolean());
                } else {
                    throw new ProtocolException("Parameter '" + parameterName +
                            "' has invalid type, requests can only contain numbers, booleans and strings.");
                }
            }
        } else {
            requestBuffer.writeByte(0);
        }

        final long requestSize = requestBuffer.size();
        if (requestSize > REQUEST_SIZE_LIMIT_BYTES) {
            throw new ProtocolException("The request is too big, max allowed size is 65535 bytes, current is " + requestSize);
        }

        sink.writeShortLe((int) requestSize);
        sink.write(requestBuffer, requestBuffer.size());
        if (hasBinaryData) {

            data.writeTo(sink);
        }

        sink.flush();
        return requestSize + binaryDataSize;
    }

    public Response readResponse(BufferedSource source) throws IOException {
        DecodeContext context = new DecodeContext(source);
        readNumber(context.source, 4); // Response parameters length, value not used, but still has to be read.

        ObjectValue responseValues;
        int type = readType(context.source);
        if (type == 16) {
            responseValues = readObject(context);
        } else {
            throw new ProtocolException("Response did not start with an object.");
        }

        ResponseData data = null;
        if (context.hasData) {
            data = new ResponseData(source, context.dataLength);
        }
        return new Response(responseValues, data);
    }

    private static void writeParameterTypeAndName(BufferedSink buffer, String name, int type) throws IOException {
        byte[] bytes = name.getBytes(PROTOCOL_CHARSET);
        int parameterNameLength = bytes.length;
        if (parameterNameLength > REQUEST_PARAM_NAME_LENGTH_LIMIT) {
            throw new ProtocolException("Parameter '" + name + "' is too long, should be no more than 63 characters.");
        }

        int encodedParamData = parameterNameLength | (type << 6);
        buffer.writeByte(encodedParamData);
        buffer.write(bytes);
    }

    private static void writeStringParameter(BufferedSink buffer, String name, String value) throws IOException {
        if (value != null) {
            writeParameterTypeAndName(buffer, name, REQUEST_PARAM_TYPE_STRING);
            byte[] bytes = value.getBytes(PROTOCOL_CHARSET);
            buffer.writeIntLe(bytes.length);
            buffer.write(bytes);
        }
    }

    private static void writeNumberParameter(BufferedSink buffer, String name, long value) throws IOException {
        if (value < 0) {
            writeStringParameter(buffer, name, String.valueOf(value));
        } else {
            writeParameterTypeAndName(buffer, name, REQUEST_PARAM_TYPE_NUMBER);
            buffer.writeLongLe(value);
        }
    }

    private static void writeBooleanParameter(BufferedSink buffer, String name, boolean value) throws IOException {
        writeParameterTypeAndName(buffer, name, REQUEST_PARAM_TYPE_BOOLEAN);
        buffer.writeByte(value ? 1 : 0);
    }

    private static void writeMethodName(BufferedSink buffer, String name, boolean requestHasBlobData) throws IOException {
        byte[] bytes = name.getBytes(PROTOCOL_CHARSET);

        int methodNameLength = bytes.length;
        if (methodNameLength > (127)) {
            throw new ProtocolException("Method name cannot be longer than 127 characters.");
        }

        if (requestHasBlobData) {
            methodNameLength = methodNameLength | (1 << REQUEST_BINARY_DATA_FLAG_POSITION);
        }

        buffer.writeByte(methodNameLength);
        buffer.write(bytes);
    }
    private static class DecodeContext {
        private List<String> stringCache = new ArrayList<>();
        private long dataLength;
        private boolean hasData;

        private BufferedSource source;
        public DecodeContext(BufferedSource source) {
            this.source = source;
        }

    }

    private static Value readValue(int type, DecodeContext context) throws IOException {
        BufferedSource source = context.source;
        if (type >= 8 && type <= 15) {
            // Number, may a 1-8 byte long integer.
            return new NumberValue(readNumber(source, type - 7));
        } else if (type == 16) {
            // Object
            return readObject(context);
        } else if (type == 17) {
            // Array
            return readArray(context);
        } else if (type >= 18 && type <= 19) {
            // Boolean, 18 means 'false', 19 is 'true'
            return new BooleanValue(type - 18 > 0);
        } else if (type == 20) {
            // Data
            return readBinaryDataInfo(context);
        } else if (type >= 200 && type <= 219) {
            // Number, with compression optimization
            return new NumberValue(type - 200);
        } else {
            return new StringValue(readStringValue(type, context));
        }
    }

    private static NumberValue readBinaryDataInfo(DecodeContext context) throws IOException {
        if (!context.hasData) {
            context.hasData = true;
            context.dataLength = readNumber(context.source, 8);
            return new NumberValue(context.dataLength);
        } else {
            throw new ProtocolException("More than one 'data' parameter in response.");
        }
    }

    private static int readType(BufferedSource source) throws IOException {
        return (int) readNumber(source, 1);
    }

    private static long readNumber(BufferedSource source, int byteCount) throws IOException {
        source.require(byteCount);
        byte[] bytes = source.readByteArray(byteCount);
        long value = 0;
        long m = 1;
        for (int i = 0; i < bytes.length; i++) {
            value += m * (bytes[i] & 0xff);
            m *= 256;
        }
        return value;
    }

    private static ValueArray readArray(DecodeContext context) throws IOException {
        List<Value> values = new ArrayList<Value>();
        int type;
        BufferedSource source = context.source;
        while ((type = readType(source)) != 255) {
            values.add(readValue(type, context));
        }

        return new ValueArray(values);
    }

    private static ObjectValue readObject(DecodeContext context) throws IOException {
        ObjectValue element = new ObjectValue();
        int type;
        while ((type = readType(context.source)) != 255) {
            element.put(readStringValue(type, context), readValue(readType(context.source), context));
        }

        return element;
    }

    private static String readStringValue(int type, DecodeContext context) throws IOException {
        BufferedSource source = context.source;
        if (type >= 0 && type <= 3) {
            // String
            long stringLength = readNumber(source, type + 1);
            String value = source.readString(stringLength, PROTOCOL_CHARSET);
            context.stringCache.add(value);
            return value;
        } else if (type >= 4 && type <= 7) {
            // String, existing value
            int cachedStringId = (int) readNumber(source, type - 3);
            return context.stringCache.get(cachedStringId);
        } else if (type >= 100 && type <= 149) {
            // String, with compression optimization
            int stringLength = type - 100;
            String value = source.readString(stringLength, PROTOCOL_CHARSET);
            context.stringCache.add(value);
            return value;
        } else if (type >= 150 && type <= 199) {
            // String, existing value, with compression optimization
            return context.stringCache.get(type - 150);
        } else {
            throw new ProtocolException("Unkown value type " + type);
        }
    }



}
