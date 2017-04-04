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

import okio.BufferedSource;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.*;

import static com.pcloud.IOUtils.closeQuietly;
import static com.pcloud.protocol.streaming.TypeToken.*;

public class BytesReader implements ProtocolReader {

    private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");

    private static final int TYPE_STRING_START = 0;
    private static final int TYPE_STRING_END = 3;
    private static final int TYPE_STRING_REUSED_START = 4;
    private static final int TYPE_STRING_REUSED_END = 7;
    private static final int TYPE_NUMBER_START = 8;
    private static final int TYPE_NUMBER_END = 15;
    private static final int TYPE_BEGIN_OBJECT = 16;
    private static final int TYPE_BEGIN_ARRAY = 17;
    private static final int TYPE_BOOLEAN_TRUE = 18;
    private static final int TYPE_BOOLEAN_FALSE = 19;
    private static final int TYPE_DATA = 20;
    private static final int TYPE_STRING_COMPRESSED_START = 100;
    private static final int TYPE_STRING_COMPRESSED_END = 149;
    private static final int TYPE_STRING_COMPRESSED_REUSED_BEGIN = 150;
    private static final int TYPE_STRING_COMPRESSED_REUSED_END = 199;
    private static final int TYPE_NUMBER_COMPRESSED_START = 200;
    private static final int TYPE_NUMBER_COMPRESSED_END = 219;
    private static final int TYPE_END_ARRAY_OBJECT = 255;

    // Types for internal use, not part of the binary protocol.
    private static final int TYPE_AGGREGATE_STRING = -2;
    private static final int TYPE_AGGREGATE_BOOLEAN = -3;
    private static final int TYPE_AGGREGATE_NUMBER = -4;
    private static final int TYPE_AGGREGATE_END_ARRAY = -5;
    private static final int TYPE_AGGREGATE_END_OBJECT = -6;

    // Scope types
    private static final int SCOPE_NONE = -1;
    private static final int SCOPE_RESPONSE = 1;
    private static final int SCOPE_OBJECT = 2;
    private static final int SCOPE_ARRAY = 3;
    private static final int UNKNOWN_SIZE = -1;

    private Deque<Integer> scopeStack = new ArrayDeque<>(10);
    private List<String> stringCache = new ArrayList<>();

    private BufferedSource source;
    private boolean responseStarted;
    private volatile long dataLength = UNKNOWN_SIZE;

    public BytesReader(BufferedSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Source argument cannot be null.");
        }
        this.source = source;
    }

    @Override
    public TypeToken peek() throws IOException {
        return getToken(peekType());
    }

    @Override
    public long beginResponse() throws IOException {
        responseStarted = true;
        long paramLength = pullNumber(4);
        scopeStack.push(SCOPE_RESPONSE);
        return paramLength;
    }

    @Override
    public long endResponse() throws IOException {
        int scope = scopeStack.peek();
        if (responseStarted && scope == SCOPE_RESPONSE) {
            while (hasNext()) {
                skipValue();
            }
            scopeStack.clear();
            stringCache.clear();
            if (dataLength == UNKNOWN_SIZE) {
                dataLength = 0;
            }
        } else {
            throw new IllegalStateException("Trying to end a response, but current scope is " + scopeName(scope));
        }

        return dataLength;
    }

    @Override
    public void beginObject() throws IOException {
        int type = pullType();
        if (type == TYPE_BEGIN_OBJECT) {
            scopeStack.push(SCOPE_OBJECT);
        } else if (type == TYPE_BEGIN_ARRAY && peekType() == TYPE_END_ARRAY_OBJECT) {
            /*
            * Empty objects are returned as empty arrays.
            * */
            pullType();
        } else {
            throw typeMismatchError(TYPE_BEGIN_OBJECT, type);
        }
    }

    @Override
    public void beginArray() throws IOException {
        int type = pullType();
        if (type == TYPE_BEGIN_ARRAY) {
            scopeStack.push(SCOPE_ARRAY);
        } else {
            throw typeMismatchError(TYPE_BEGIN_ARRAY, type);
        }
    }

    @Override
    public void endArray() throws IOException {
        int type = pullType();
        if (type == TYPE_END_ARRAY_OBJECT) {
            int scope = scopeStack.peek();
            if (scope == SCOPE_ARRAY) {
                scopeStack.pop();
            } else {
                throw new IllegalStateException("Trying to close an array, but current scope is " + scopeName(scope));
            }
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_END_ARRAY, type);
        }
    }

    @Override
    public void endObject() throws IOException {
        int type = pullType();
        if (type == TYPE_END_ARRAY_OBJECT) {
            int scope = getCurrentScope();
            if (scope == SCOPE_OBJECT) {
                scopeStack.pop();
            } else {
                throw new IllegalStateException("Trying to close an object, but current scope is " + scopeName(scope));
            }
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_END_OBJECT, type);
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        int type = pullType();
        if (type == TYPE_BOOLEAN_TRUE) {
            return true;
        } else if (type == TYPE_BOOLEAN_FALSE) {
            return false;
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_BOOLEAN, type);
        }
    }

    @Override
    public String readString() throws IOException {
        int type = pullType();
        if (type >= TYPE_STRING_START && type <= TYPE_STRING_END) {
            // String
            long stringLength = pullNumber(type + 1);
            String value = source.readString(stringLength, PROTOCOL_CHARSET);
            stringCache.add(value);
            return value;
        } else if (type >= TYPE_STRING_REUSED_START && type <= TYPE_STRING_REUSED_END) {
            // String, existing value
            int cachedStringId = (int) pullNumber(type - 3);
            return stringCache.get(cachedStringId);
        } else if (type >= TYPE_STRING_COMPRESSED_START && type <= TYPE_STRING_COMPRESSED_END) {
            // String, with compression optimization
            int stringLength = type - 100;
            String value = source.readString(stringLength, PROTOCOL_CHARSET);
            stringCache.add(value);
            return value;
        } else if (type >= TYPE_STRING_COMPRESSED_REUSED_BEGIN && type <= TYPE_STRING_COMPRESSED_REUSED_END) {
            // String, existing value, with compression optimization
            return stringCache.get(type - 150);
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_STRING, type);
        }
    }

    @Override
    public long readNumber() throws IOException {
        int type = pullType();
        if (type >= TYPE_NUMBER_START && type <= TYPE_NUMBER_END) {
            // Number, may a 1-8 byte long integer.
            return pullNumber(type - 7);
        } else if (type >= TYPE_NUMBER_COMPRESSED_START && type <= TYPE_NUMBER_COMPRESSED_END) {
            // Number, with compression optimization
            return type - 200;
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_NUMBER, type);
        }
    }

    @Override
    public void close() {
        closeQuietly(source);
    }

    private int peekType() throws IOException {
        source.require(1);
        int type = source.buffer().getByte(0) & 0xff;
        if (type == TYPE_DATA) {
            // Swallow the token, and peek again.
            source.readByte();
            dataLength = pullNumber(8);
            return peekType();
        }
        return type;
    }

    private int pullType() throws IOException {
        if (responseStarted) {
            int type = source.readByte() & 0xff;
            if (type == TYPE_DATA) {
                // Store the data content length and
                // swallow the token.
                dataLength = pullNumber(8);
                return pullType();
            }
            return type;
        } else {
            throw new IllegalStateException("First call beginResponse().");
        }
    }

    private long pullNumber(int byteCount) throws IOException {
        source.require(byteCount);
        if (byteCount > 1) {
            byte[] number = source.readByteArray(byteCount);
            long value = 0;
            long m = 1;
            for (int i = 0; i < byteCount; i++) {
                value += m * (number[i] & 0xff);
                m *= 256;
            }
            return value;
        } else {
            return source.readByte() & 0xff;
        }
    }

    private static RuntimeException typeMismatchError(int expectedType, int actualType) {
        throw new IllegalStateException("Expected " + typeName(expectedType) + ", but was " + typeName(actualType));
    }

    private TypeToken getToken(int type) throws ProtocolException {
        if (type >= TYPE_NUMBER_START && type <= TYPE_NUMBER_END ||
                type >= TYPE_NUMBER_COMPRESSED_START && type <= TYPE_NUMBER_COMPRESSED_END) {
            // Number, it may be a 1-8 byte long integer or an index for
            // number types with compression optimization
            return NUMBER;
        } else if (type >= TYPE_STRING_START && type <= TYPE_STRING_END ||
                type >= TYPE_STRING_COMPRESSED_START && type <= TYPE_STRING_COMPRESSED_REUSED_END) {
            // Number, it may be a 1-8 byte long integer or an index for
            // number types with compression optimization
            return STRING;
        } else if (type == TYPE_BEGIN_OBJECT) {
            // Object
            return BEGIN_OBJECT;
        } else if (type == TYPE_BEGIN_ARRAY) {
            // Array
            return BEGIN_ARRAY;
        } else if (type >= TYPE_BOOLEAN_TRUE && type <= TYPE_BOOLEAN_FALSE) {
            // Boolean
            return TypeToken.BOOLEAN;
        } else if (type == TYPE_END_ARRAY_OBJECT) {
            return scopeStack.peek() == TYPE_BEGIN_OBJECT ? END_OBJECT : END_ARRAY;
        } else {
            throw new ProtocolException("Unknown type " + type);
        }
    }

    private static String typeName(final int type) {
        if (type == TYPE_AGGREGATE_NUMBER) {
            // Number, may a 1-8 byte long integer.
            // Number, with compression optimization
            return NUMBER.toString();
        } else if (type == TYPE_BEGIN_OBJECT) {
            // Object
            return BEGIN_OBJECT.toString();
        } else if (type == TYPE_BEGIN_ARRAY) {
            // Array
            return BEGIN_ARRAY.toString();
        } else if (type == TYPE_AGGREGATE_BOOLEAN) {
            // Boolean, 18 means 'false', 19 is 'true'
            return BOOLEAN.toString();
        } else if (type == TYPE_AGGREGATE_END_ARRAY) {
            return END_ARRAY.toString();
        } else if (type == TYPE_AGGREGATE_END_OBJECT) {
            return END_OBJECT.toString();
        } else {
            return "(Unknown type " + type + ")";
        }
    }

    private static String scopeName(final int scope) {
        switch (scope) {
            case SCOPE_RESPONSE:
                return "Response";
            case SCOPE_ARRAY:
                return "Array";
            case SCOPE_OBJECT:
                return "Object";
            default:
                return "Unknown";
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        return peekType() != TYPE_END_ARRAY_OBJECT;
    }

    @Override
    public long dataContentLength() {
        return dataLength;
    }

    @Override
    public void skipValue() throws IOException {
        final int type = peekType();
        if (type >= TYPE_NUMBER_START && type <= TYPE_NUMBER_END) {
            // Skip 1-8 bytes depending on number size + 1 byte for the type.
            // [type] - 7 + 1
            source.skip(type - 6);
        } else if (type >= TYPE_NUMBER_COMPRESSED_START && type <= TYPE_NUMBER_COMPRESSED_END) {
            source.skip(1);
        } else if (type >= TYPE_STRING_START && type <= TYPE_STRING_END ||
                type >= TYPE_STRING_COMPRESSED_START && type <= TYPE_STRING_COMPRESSED_REUSED_END) {
            // The compression optimizations for strings require full reading.
            readString();
        } else if (type == TYPE_BOOLEAN_TRUE || type == TYPE_BOOLEAN_FALSE) {
            source.skip(1);
        }
        else if (type == TYPE_BEGIN_OBJECT) {
            // Object
            beginObject();
            while (hasNext()) {
                skipValue();
            }
            endObject();
        } else if (type == TYPE_BEGIN_ARRAY) {
            // Array
            beginArray();
            while (hasNext()) {
                skipValue();
            }
            endArray();
        } else if (type == TYPE_DATA) {
            // Read the 8-byte length of the attached data
            dataLength = pullNumber(8);
        } else if (type == TYPE_END_ARRAY_OBJECT) {
            int scope = getCurrentScope();
            switch (scope) {
                case TYPE_BEGIN_OBJECT:
                    endObject();
                    break;
                case TYPE_BEGIN_ARRAY:
                    endArray();
            }
        } else {
            throw new ProtocolException("Unknown type " + type);
        }
    }

    private int getCurrentScope() {
        Integer scope = scopeStack.peek();
        return scope != null ? scope : SCOPE_NONE;
    }
}
