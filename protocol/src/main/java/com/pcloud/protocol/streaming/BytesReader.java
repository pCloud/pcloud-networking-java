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

package com.pcloud.protocol.streaming;

import com.pcloud.IOUtils;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Locale;

import static com.pcloud.IOUtils.closeQuietly;
import static com.pcloud.protocol.streaming.TypeToken.*;

public class BytesReader implements ProtocolResponseReader {

    private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");

    private static final int TYPE_STRING_START = 0;
    private static final int TYPE_STRING_END = 3;
    private static final int TYPE_STRING_REUSED_START = 4;
    private static final int TYPE_STRING_REUSED_END = 7;
    private static final int TYPE_NUMBER_START = 8;
    private static final int TYPE_NUMBER_END = 15;
    private static final int TYPE_BEGIN_OBJECT = 16;
    private static final int TYPE_BEGIN_ARRAY = 17;
    private static final int TYPE_BOOLEAN_FALSE = 18;
    private static final int TYPE_BOOLEAN_TRUE = 19;
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
    public static final int DEFAULT_STRING_CACHE_SIZE = 50;

    private static final int RESPONSE_LENGTH_BYTES = 4;
    private static final int SCOPE_STACK_INITIAL_CAPACITY = 5;
    private static final int HEX_255 = 0xff;
    private static final int DATA_LENGTH = 8;
    private static final int NUMBER_BYTES_SKIP = 6;
    private static final int READ_STRING_EXISTING_VALUE = 3;
    private static final int READ_STRING_COMPRESSED = 100;
    private static final int READ_STRING_COMPRESSED_EXISTING_VALUE = 150;
    private static final int NUMBER_COMPRESSED = 200;
    private static final int NUMBER_NON_COMPRESSED = 7;
    private static final int NEXT_BYTE_POSITION = 256;

    private int currentScope = SCOPE_NONE;
    private int previousScope = SCOPE_NONE;
    private Deque<Integer> scopeStack = new ArrayDeque<>(SCOPE_STACK_INITIAL_CAPACITY);
    private BufferedSource bufferedSource;

    private int lastStringId;
    private String[] stringCache;
    private volatile long dataLength = UNKNOWN_SIZE;

    private BytesReader() {

    }

    public BytesReader(BufferedSource bufferedSource) {
        if (bufferedSource == null) {
            throw new IllegalArgumentException("Source argument cannot be null.");
        }
        this.bufferedSource = bufferedSource;
    }

    @Override
    public TypeToken peek() throws IOException {
        return getToken(peekType());
    }

    @Override
    public long beginResponse() throws IOException {
        if (currentScope == SCOPE_NONE) {
            pushScope(SCOPE_RESPONSE);
            stringCache = new String[DEFAULT_STRING_CACHE_SIZE];
            lastStringId = 0;
            return pullNumber(RESPONSE_LENGTH_BYTES);
        }

        throw new SerializationException("Trying to start reading a response, which has already been started.");
    }

    @Override
    public long endResponse() throws IOException {
        if (currentScope != SCOPE_RESPONSE) {
            if (currentScope == SCOPE_NONE) {
                throw new SerializationException("Trying to end a response " +
                                                         "but none is being read, first call beginResponse()");
            } else {
                throw new SerializationException("Trying to end a response, " +
                                                         "but current scope is " + scopeName(currentScope));
            }
        }

        // Check if anything was read and skip it until end.
        if (previousScope == SCOPE_NONE) {
            while (hasNext()) {
                skipValue();
            }
        }
        popScope();
        stringCache = null;

        if (dataLength == UNKNOWN_SIZE) {
            dataLength = 0;
        }

        return dataLength;
    }

    @Override
    public void beginObject() throws IOException {
        int type = pullType();
        if (type == TYPE_BEGIN_OBJECT ||
                    (type == TYPE_BEGIN_ARRAY &&
                             peekType() == TYPE_END_ARRAY_OBJECT)) {
            pushScope(SCOPE_OBJECT);
        } else {
            throw typeMismatchError(TYPE_BEGIN_OBJECT, type);
        }
    }

    @Override
    public void beginArray() throws IOException {
        int type = pullType();
        if (type == TYPE_BEGIN_ARRAY) {
            pushScope(SCOPE_ARRAY);
        } else {
            throw typeMismatchError(TYPE_BEGIN_ARRAY, type);
        }
    }

    @Override
    public void endArray() throws IOException {
        int type = pullType();
        if (type == TYPE_END_ARRAY_OBJECT) {
            if (currentScope == SCOPE_ARRAY) {
                popScope();
            } else {
                throw new IllegalStateException("Trying to close an array, but current scope is " +
                                                        scopeName(currentScope));
            }
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_END_ARRAY, type);
        }
    }

    @Override
    public void endObject() throws IOException {
        int type = pullType();
        if (type == TYPE_END_ARRAY_OBJECT) {
            if (currentScope == SCOPE_OBJECT) {
                popScope();
            } else {
                throw new IllegalStateException("Trying to close an object, but current scope is " +
                                                        scopeName(currentScope));
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
            String value = bufferedSource.readUtf8(stringLength);
            cacheString(value);
            return value;
        } else if (type >= TYPE_STRING_REUSED_START && type <= TYPE_STRING_REUSED_END) {
            // String, existing value
            int cachedStringId = (int) pullNumber(type - READ_STRING_EXISTING_VALUE);
            return stringCache[cachedStringId];
        } else if (type >= TYPE_STRING_COMPRESSED_START && type <= TYPE_STRING_COMPRESSED_END) {
            // String, with compression optimization
            int stringLength = type - READ_STRING_COMPRESSED;
            String value = bufferedSource.readUtf8(stringLength);
            cacheString(value);
            return value;
        } else if (type >= TYPE_STRING_COMPRESSED_REUSED_BEGIN && type <= TYPE_STRING_COMPRESSED_REUSED_END) {
            // String, existing value, with compression optimization
            return stringCache[type - READ_STRING_COMPRESSED_EXISTING_VALUE];
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_STRING, type);
        }
    }

    private void cacheString(String string) {
        if (stringCache.length == lastStringId + 1) {
            stringCache = Arrays.copyOf(stringCache, stringCache.length * 2);
        }

        stringCache[lastStringId++] = string;
    }

    @Override
    public long readNumber() throws IOException {
        int type = pullType();
        if (type >= TYPE_NUMBER_START && type <= TYPE_NUMBER_END) {
            // Number, may a 1-8 byte long integer.
            return pullNumber(type - NUMBER_NON_COMPRESSED);
        } else if (type >= TYPE_NUMBER_COMPRESSED_START && type <= TYPE_NUMBER_COMPRESSED_END) {
            // Number, with compression optimization
            return type - NUMBER_COMPRESSED;
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_NUMBER, type);
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        return !(currentScope == SCOPE_RESPONSE && previousScope != SCOPE_NONE) &&
                       peekType() != TYPE_END_ARRAY_OBJECT;
    }

    @Override
    public long dataContentLength() {
        return dataLength;
    }

    @Override
    public ProtocolResponseReader newPeekingReader() {
        BytesReader reader = new PeekingByteReader();
        reader.currentScope = this.currentScope;
        reader.previousScope = this.previousScope;
        reader.scopeStack = new ArrayDeque<>(this.scopeStack);
        reader.bufferedSource = Okio.buffer(new PeekingSource(this.bufferedSource, 0L));
        reader.stringCache = this.stringCache;
        reader.dataLength = this.dataLength;
        reader.lastStringId = this.lastStringId;
        /*
        * Swap the BufferedSource for an implementation that reads ahead the byte stream
        * without consuming it.
        * */
        return reader;
    }

    @Override
    public void skipValue() throws IOException {

        if (currentScope == SCOPE_NONE) {
            throw new IllegalStateException("Trying to skipValue, but currentScope is " + currentScope +
                                                    ". You must call beginResponse() first.");
        }

        final int type = peekType();
        if (type >= TYPE_NUMBER_START && type <= TYPE_NUMBER_END) {
            // Skip 1-8 bytes depending on number size + 1 byte for the type.
            // skip ([type] - 7) + 1 bytes
            bufferedSource.skip(type - NUMBER_BYTES_SKIP);
        } else if (type >= TYPE_NUMBER_COMPRESSED_START && type <= TYPE_NUMBER_COMPRESSED_END) {
            bufferedSource.skip(1);
        } else if (type >= TYPE_STRING_REUSED_START && type <= TYPE_STRING_REUSED_END) {
            // Index to a previously read string, 1-4 bytes
            // skip ([type] - 3) + 1 bytes.
            bufferedSource.skip(type - 2);
        } else if (type >= TYPE_STRING_START && type <= TYPE_STRING_END ||
                           type >= TYPE_STRING_COMPRESSED_START && type <= TYPE_STRING_COMPRESSED_REUSED_END) {
            // The compression optimizations for strings require full reading.
            readString();
        } else if (type == TYPE_BOOLEAN_TRUE || type == TYPE_BOOLEAN_FALSE) {
            bufferedSource.skip(1);
        } else if (type == TYPE_BEGIN_OBJECT) {
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
            dataLength = pullNumber(DATA_LENGTH);
        } else if (type == TYPE_END_ARRAY_OBJECT) {
            int scope = currentScope();
            switch (scope) {
                case TYPE_BEGIN_OBJECT:
                    endObject();
                    break;
                case TYPE_BEGIN_ARRAY:
                default:
                    endArray();

            }
        } else {
            throw new ProtocolException("Unknown type " + type);
        }
    }

    @Override
    public int currentScope() {
        return currentScope;
    }

    @Override
    public void close() {
        closeQuietly(bufferedSource);
    }

    private void pushScope(int newScope) {
        previousScope = currentScope;
        scopeStack.push(newScope);
        currentScope = newScope;
    }

    private void popScope() {
        if (currentScope != SCOPE_NONE) {
            previousScope = scopeStack.pop();
            currentScope = scopeStack.isEmpty() ? SCOPE_NONE : scopeStack.peek();
        }
    }

    private int peekType() throws IOException {
        bufferedSource.require(1);
        int type = bufferedSource.buffer().getByte(0) & HEX_255;
        if (type == TYPE_DATA) {
            dataLength = IOUtils.peekNumberLe(bufferedSource, 1, DATA_LENGTH);
            return TYPE_NUMBER_END;
        }
        return type;
    }

    private int pullType() throws IOException {
        if (currentScope == SCOPE_NONE) {
            throw new IllegalStateException("First call beginResponse().");
        }
        int type = bufferedSource.readByte() & HEX_255;
        if (type == TYPE_DATA) {
            dataLength = IOUtils.peekNumberLe(bufferedSource, 1, DATA_LENGTH);
            return TYPE_NUMBER_END;
        }
        return type;
    }

    private long pullNumber(int byteCount) throws IOException {
        bufferedSource.require(byteCount);
        if (byteCount > 1) {
            byte[] number = bufferedSource.readByteArray(byteCount);
            long value = 0;
            long m = 1;
            for (int i = 0; i < byteCount; i++) {
                value += m * (number[i] & HEX_255);
                m *= NEXT_BYTE_POSITION;
            }
            return value;
        } else {
            return bufferedSource.readByte() & HEX_255;
        }
    }

    private SerializationException typeMismatchError(int expectedType, int actualType) {
        return new SerializationException("Expected '" +
                                                  typeName(expectedType) +
                                                  "', but was '" +
                                                  typeName(actualType) +
                                                  "'.");
    }

    private TypeToken getToken(int type) throws SerializationException {
        if (type >= TYPE_NUMBER_START && type <= TYPE_NUMBER_END ||
                    type >= TYPE_NUMBER_COMPRESSED_START && type <= TYPE_NUMBER_COMPRESSED_END) {
            // Number, it may be a 1-8 byte long integer or an index for
            // number types with compression optimization
            return NUMBER;
        } else if ((type >= TYPE_STRING_START && type <= TYPE_STRING_END) ||
                           (type >= TYPE_STRING_REUSED_START && type <= TYPE_STRING_REUSED_END) ||
                           (type >= TYPE_STRING_COMPRESSED_START && type <= TYPE_STRING_COMPRESSED_REUSED_END)) {
            // Number, it may be a 1-8 byte long integer or an index for
            // number types with compression optimization
            return STRING;
        } else if (type == TYPE_BEGIN_OBJECT) {
            // Object
            return BEGIN_OBJECT;
        } else if (type == TYPE_BEGIN_ARRAY) {
            // Array
            return BEGIN_ARRAY;
        } else if (type >= TYPE_BOOLEAN_FALSE && type <= TYPE_BOOLEAN_TRUE) {
            // Boolean
            return TypeToken.BOOLEAN;
        } else if (type == TYPE_END_ARRAY_OBJECT) {
            return currentScope == TYPE_BEGIN_OBJECT ? END_OBJECT : END_ARRAY;
        } else {
            throw new SerializationException("Unknown type " + type);
        }
    }

    private String typeName(final int type) {
        TypeToken typeToken;
        if (type == TYPE_AGGREGATE_NUMBER) {
            // Number, may a 1-8 byte long integer.
            // Number, with compression optimization
            typeToken = NUMBER;
        } else if (type == TYPE_BEGIN_OBJECT) {
            // Object
            typeToken = BEGIN_OBJECT;
        } else if (type == TYPE_BEGIN_ARRAY) {
            // Array
            typeToken = BEGIN_ARRAY;
        } else if (type == TYPE_AGGREGATE_BOOLEAN) {
            // Boolean, 18 means 'false', 19 is 'true'
            typeToken = BOOLEAN;
        } else if (type == TYPE_AGGREGATE_END_ARRAY) {
            typeToken = END_ARRAY;
        } else if (type == TYPE_AGGREGATE_END_OBJECT) {
            typeToken = END_OBJECT;
        } else {
            try {
                typeToken = getToken(type);
            } catch (SerializationException e) {
                return "(Unknown type " + type + ")";
            }
        }

        return typeToken.toString().toUpperCase(Locale.ENGLISH);
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

    private static class PeekingByteReader extends BytesReader {

        private PeekingByteReader() {
            super();
        }

        @Override
        public ProtocolResponseReader newPeekingReader() {
            throw new IllegalStateException("Cannot call newPeekingReader(), this reader is already non-consuming.");
        }
    }
}
