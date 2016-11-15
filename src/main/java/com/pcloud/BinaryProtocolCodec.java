package com.pcloud;

import com.pcloud.value.*;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Source;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Georgi Neykov on 11/10/2016.
 */

class BinaryProtocolCodec {

    private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");
    private static final int REQUEST_PARAM_TYPE_STRING = 0;
    private static final int REQUEST_PARAM_TYPE_NUMBER = 1;
    private static final int REQUEST_PARAM_TYPE_BOOLEAN = 2;
    private static final int REQUEST_PARAM_TYPE_LIMIT = 3;
    private static final int REQUEST_PARAM_COUNT_LIMIT = 255;
    private static final int REQUEST_PARAM_NAME_LENGTH_LIMIT = 63;
    private static final int REQUEST_SIZE_LIMIT_BYTES = 65535;
    private static final int REQUEST_BINARY_DATA_FLAG_POSITION = 7;

    public void writeRequest(BufferedSink sink, String methodName, ObjectValue element) throws IOException {
        writeRequest(sink, methodName, element, null, -1);
    }

    public void writeRequest(BufferedSink sink, String methodName, ObjectValue element, Source data, long dataLength) throws IOException {
        if (data != null && dataLength < 0) {
            throw new IOException("Invalid data length value of " + dataLength);
        }

        Buffer requestBuffer = new Buffer();
        boolean hasBinaryData = data != null && dataLength > 0;
        writeMethodName(requestBuffer, methodName, hasBinaryData);
        if (hasBinaryData) {
            requestBuffer.writeLongLe(dataLength);
        }

        if (element != null) {
            Set<Map.Entry<String, Value>> parameters = element.propertySet();
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

        Buffer finalBuffer = new Buffer();

        finalBuffer.writeShortLe((int) requestSize);
        finalBuffer.write(requestBuffer, requestBuffer.size());
        sink.writeAll(finalBuffer);
        if (hasBinaryData) {
            Buffer buffer = new Buffer();
            long bytesRead, totalBytesRead = 0;
            while ((bytesRead = data.read(buffer, 4096)) != 1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > dataLength) {
                    throw new IOException("Declared data size is less than the actual data source length.");
                }
                sink.write(buffer, bytesRead);
            }

            if (totalBytesRead < dataLength) {
                throw new IOException("Declared data size is bigger than the actual data source length.");
            }
        }

        sink.flush();
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

    public ObjectValue readResponse(BufferedSource source) throws IOException {
        List<String> stringsCache = new ArrayList<String>();
        long responseLength = readNumber(source, 4);
        int type = readType(source);

        if (type == 16) {
            return (ObjectValue) readValue(source, type, stringsCache);
        } else {
            throw new ProtocolException("Response did not start with an object.");
        }
    }

    private static Value readValue(BufferedSource source, int type, List<String> stringCache) throws IOException {
        if (type >= 8 && type <= 15) {
            // Number, may a 1-8 byte long integer.
            return new NumberValue(readNumber(source, type - 7));
        } else if (type == 16) {
            // Object
            return readObject(source, stringCache);
        } else if (type == 17) {
            // Array
            return readArray(source, stringCache);
        } else if (type >= 18 && type <= 19) {
            // Boolean, 18 means 'false', 19 is 'true'
            return new BooleanValue(type - 18 > 0);
        } else if (type == 20) {
            // Data
            throw new UnsupportedOperationException("No implemented yet.");
        } else if (type >= 200 && type <= 219) {
            // Number, with compression optimization
            return new NumberValue(type - 200);
        } else {
            return new StringValue(readStringValue(source, type, stringCache));
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

    private static ValueArray readArray(BufferedSource source, List<String> stringCache) throws IOException {
        List<Value> values = new ArrayList<Value>();
        int type;
        while ((type = readType(source)) != 255) {
            values.add(readValue(source, type, stringCache));
        }

        return new ValueArray(values);
    }

    private static ObjectValue readObject(BufferedSource source, List<String> stringCache) throws IOException {
        ObjectValue element = new ObjectValue();
        int type;
        while ((type = readType(source)) != 255) {
            element.put(readStringValue(source, type, stringCache), readValue(source, readType(source), stringCache));
        }

        return element;
    }

    private static String readStringValue(BufferedSource source, int type, List<String> stringCache) throws IOException {
        if (type >= 0 && type <= 3) {
            // String
            long stringLength = readNumber(source, type + 1);
            String value = source.readString(stringLength, PROTOCOL_CHARSET);
            stringCache.add(value);
            return value;
        } else if (type >= 4 && type <= 7) {
            // String, existing value
            int cachedStringId = (int) readNumber(source, type - 3);
            return stringCache.get(cachedStringId);
        } else if (type >= 100 && type <= 149) {
            // String, with compression optimization
            int stringLength = type - 100;
            String value = source.readString(stringLength, PROTOCOL_CHARSET);
            stringCache.add(value);
            return value;
        } else if (type >= 150 && type <= 199) {
            // String, existing value, with compression optimization
            return stringCache.get(type - 150);
        } else {
            throw new ProtocolException("Unkown value type " + type);
        }
    }

}
