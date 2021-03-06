/*
 * Copyright (c) 2020 pCloud AG
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

import com.pcloud.utils.IOUtils;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.Locale;

import static com.pcloud.utils.IOUtils.closeQuietly;

/**
 * A reader for pCloud's binary data protocol
 * <p>
 * An implementation of {@linkplain ProtocolResponseReader} which can read data
 * encoded in pCloud's binary protocol from a stream of bytes in pull-based fashion
 * with minimal overhead.
 * <p>
 * The implementation can be reused for reading multiple responses from a byte source,
 * as long as they are read completely.
 *
 * @see ProtocolReader
 * @see ProtocolResponseReader
 */
public class BytesReader implements ProtocolResponseReader {

    private static final int OFFSET_EXISTING_STRING_CACHE = 3;
    private static final int OFFSET_NUMBER_NON_COMPRESSED = 7;
    private static final int OFFSET_NUMBER_COMPRESSED = 200;
    private static final int OFFSET_READ_STRING_COMPRESSED_EXISTING_VALUE = 150;
    private static final int OFFSET_READ_STRING_COMPRESSED = 100;

    // Types for internal use, not part of the binary protocol.
    private static final int TYPE_AGGREGATE_STRING = -2;
    private static final int TYPE_AGGREGATE_END_OBJECT = -6;
    private static final int TYPE_AGGREGATE_END_ARRAY = -5;
    private static final int TYPE_AGGREGATE_NUMBER = -4;
    private static final int TYPE_AGGREGATE_BOOLEAN = -3;

    private static final int NUMBER_SKIP = 6;
    private static final int DEFAULT_STRING_CACHE_SIZE = 50;
    private static final int SCOPE_STACK_INITIAL_CAPACITY = 5;

    private static final int HEX_255 = 0xff;

    private volatile int currentScope = SCOPE_NONE;
    private int previousScope = SCOPE_NONE;
    private IntStack scopeStack = new IntStack(SCOPE_STACK_INITIAL_CAPACITY);
    private BufferedSource bufferedSource;

    private int lastStringId;
    private String[] stringCache;
    private volatile long dataLength = UNKNOWN_SIZE;

    private BytesReader() {
    }

    /**
     * Create a {@linkplain BytesReader} instance
     * <p>
     *
     * @param bufferedSource a {@linkplain BufferedSource} to read the data from
     * @throws IllegalArgumentException on a null {@linkplain BufferedSource} argument
     */
    public BytesReader(BufferedSource bufferedSource) {
        if (bufferedSource == null) {
            throw new IllegalArgumentException("Source argument cannot be null.");
        }
        this.bufferedSource = bufferedSource;
    }

    @Override
    public TypeToken peek() throws IOException {
        checkScopeIsAtLeast(currentScope, SCOPE_RESPONSE);
        return getToken(peekType());
    }

    @Override
    public long beginResponse() throws IOException {
        checkScope(currentScope, SCOPE_NONE);
        pushScope(SCOPE_RESPONSE);
        stringCache = new String[DEFAULT_STRING_CACHE_SIZE];
        lastStringId = 0;
        return pullNumber(Protocol.SIZE_RESPONSE_LENGTH);
    }

    @Override
    public boolean endResponse() throws IOException {
        checkScope(currentScope, SCOPE_RESPONSE);

        // Check if anything was read and skip it until end.
        if (previousScope == SCOPE_NONE) {
            while (hasNext()) {
                skipValue();
            }
        }
        popScope();
        stringCache = null;

        boolean dataAvailable = dataLength != UNKNOWN_SIZE;
        if (dataAvailable) {
            pushScope(SCOPE_DATA);
        }

        return dataAvailable;
    }

    @Override
    public void beginObject() throws IOException {
        checkScopeIsAtLeast(currentScope, SCOPE_RESPONSE);
        int type = pullType();
        if (type == Protocol.TYPE_BEGIN_OBJECT ||
                (type == Protocol.TYPE_BEGIN_ARRAY &&
                        peekType() == Protocol.TYPE_END_ARRAY_OBJECT)) {
            pushScope(SCOPE_OBJECT);
        } else {
            throw typeMismatchError(Protocol.TYPE_BEGIN_OBJECT, type);
        }
    }

    @Override
    public void beginArray() throws IOException {
        checkScopeIsAtLeast(currentScope, SCOPE_OBJECT);
        int type = pullType();
        if (type == Protocol.TYPE_BEGIN_ARRAY) {
            pushScope(SCOPE_ARRAY);
        } else {
            throw typeMismatchError(Protocol.TYPE_BEGIN_ARRAY, type);
        }
    }

    @Override
    public void endArray() throws IOException {
        checkScope(currentScope, SCOPE_ARRAY);
        int type = pullType();
        if (type == Protocol.TYPE_END_ARRAY_OBJECT) {
            popScope();
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_END_ARRAY, type);
        }
    }

    @Override
    public void endObject() throws IOException {
        checkScope(currentScope, SCOPE_OBJECT);
        int type = pullType();
        if (type == Protocol.TYPE_END_ARRAY_OBJECT) {
            popScope();
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_END_OBJECT, type);
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        checkScopeIsAtLeast(currentScope, SCOPE_OBJECT);
        int type = pullType();
        if (type == Protocol.TYPE_BOOLEAN_TRUE) {
            return true;
        } else if (type == Protocol.TYPE_BOOLEAN_FALSE) {
            return false;
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_BOOLEAN, type);
        }
    }

    @Override
    public String readString() throws IOException {
        checkScopeIsAtLeast(currentScope, SCOPE_OBJECT);
        int type = pullType();
        if (type >= Protocol.TYPE_STRING_START && type <= Protocol.TYPE_STRING_END) {
            // String
            long stringLength = pullNumber(type + 1);
            String value = bufferedSource.readUtf8(stringLength);
            cacheString(value);
            return value;
        } else if (type >= Protocol.TYPE_STRING_REUSED_START && type <= Protocol.TYPE_STRING_REUSED_END) {
            // String, existing value
            int cachedStringId = (int) pullNumber(type - OFFSET_EXISTING_STRING_CACHE);
            return stringCache[cachedStringId];
        } else if (type >= Protocol.TYPE_STRING_COMPRESSED_START && type <= Protocol.TYPE_STRING_COMPRESSED_END) {
            // String, with compression optimization
            int stringLength = type - OFFSET_READ_STRING_COMPRESSED;
            String value = bufferedSource.readUtf8(stringLength);
            cacheString(value);
            return value;
        } else if (type >= Protocol.TYPE_STRING_COMPRESSED_REUSED_BEGIN &&
                type <= Protocol.TYPE_STRING_COMPRESSED_REUSED_END) {
            // String, existing value, with compression optimization
            return stringCache[type - OFFSET_READ_STRING_COMPRESSED_EXISTING_VALUE];
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_STRING, type);
        }
    }

    private void cacheString(String string) {
        if (stringCache.length == lastStringId + 1) {
            int newCapacity = stringCache.length << 1;
            if (newCapacity < 0) {
                throw new IllegalStateException("String cache size too big.");
            }

            stringCache = Arrays.copyOf(stringCache, newCapacity);
        }

        stringCache[lastStringId++] = string;
    }

    @Override
    public long readNumber() throws IOException {
        checkScopeIsAtLeast(currentScope, SCOPE_OBJECT);
        int type = pullType();
        if (type >= Protocol.TYPE_NUMBER_START && type <= Protocol.TYPE_NUMBER_END) {
            // Number, may a 1-8 byte long integer.
            return pullNumber(type - OFFSET_NUMBER_NON_COMPRESSED);
        } else if (type >= Protocol.TYPE_NUMBER_COMPRESSED_START && type <= Protocol.TYPE_NUMBER_COMPRESSED_END) {
            // Number, with compression optimization
            return type - OFFSET_NUMBER_COMPRESSED;
        } else {
            throw typeMismatchError(TYPE_AGGREGATE_NUMBER, type);
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        return !(currentScope == SCOPE_RESPONSE && previousScope != SCOPE_NONE) &&
                peekType() != Protocol.TYPE_END_ARRAY_OBJECT;
    }

    @Override
    public long dataContentLength() {
        checkScope(currentScope, SCOPE_DATA);
        return dataLength;
    }

    @Override
    public void readData(OutputStream outputStream) throws IOException {
        readData(Okio.buffer(Okio.sink(outputStream)));
    }

    @Override
    public void readData(BufferedSink sink) throws IOException {
        if (currentScope != SCOPE_DATA) {
            throw new IllegalStateException("Cannot read data," +
                    " either the response is not read fully or no data is following.");
        }

        long length = dataLength;
        sink.write(bufferedSource, length);
        dataLength = UNKNOWN_SIZE;
        popScope();
    }

    @Override
    public ProtocolResponseReader newPeekingReader() {
        BytesReader reader = new PeekingByteReader();
        reader.currentScope = this.currentScope;
        reader.previousScope = this.previousScope;
        reader.scopeStack = new IntStack(this.scopeStack);
        reader.bufferedSource = Okio.buffer(this.bufferedSource.peek());
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
        checkScopeIsAtLeast(currentScope, SCOPE_RESPONSE);

        final int type = peekType();
        if (type >= Protocol.TYPE_NUMBER_START && type <= Protocol.TYPE_NUMBER_END) {
            // Skip 1-8 bytes depending on number size + 1 byte for the type.
            // skip ([type] - 7) + 1 bytes
            bufferedSource.skip(type - NUMBER_SKIP);
        } else if (type >= Protocol.TYPE_NUMBER_COMPRESSED_START && type <= Protocol.TYPE_NUMBER_COMPRESSED_END) {
            bufferedSource.skip(1);
        } else if (type >= Protocol.TYPE_STRING_REUSED_START && type <= Protocol.TYPE_STRING_REUSED_END) {
            // Index to a previously read string, 1-4 bytes
            // skip ([type] - 3) + 1 bytes.
            bufferedSource.skip(type - 2);
        } else if (type >= Protocol.TYPE_STRING_START && type <= Protocol.TYPE_STRING_END ||
                type >= Protocol.TYPE_STRING_COMPRESSED_START && type <= Protocol.TYPE_STRING_COMPRESSED_REUSED_END) {
            // The compression optimizations for strings require full reading.
            readString();
        } else if (type == Protocol.TYPE_BOOLEAN_TRUE || type == Protocol.TYPE_BOOLEAN_FALSE) {
            bufferedSource.skip(1);
        } else if (type == Protocol.TYPE_BEGIN_OBJECT) {
            // Object
            beginObject();
            while (hasNext()) {
                skipValue();
            }
            endObject();
        } else if (type == Protocol.TYPE_BEGIN_ARRAY) {
            // Array
            beginArray();
            while (hasNext()) {
                skipValue();
            }
            endArray();
        } else if (type == Protocol.TYPE_DATA) {
            // Read the 8-byte length of the attached data
            dataLength = pullNumber(Protocol.SIZE_DATA_BYTESIZE);
        } else if (type == Protocol.TYPE_END_ARRAY_OBJECT) {
            int scope = currentScope();
            switch (scope) {
                case ProtocolReader.SCOPE_OBJECT:
                    endObject();
                    break;
                case ProtocolReader.SCOPE_ARRAY:
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

    /**
     * Close the {@linkplain BufferedSource}
     */
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
        int type = (int) IOUtils.peekNumberLe(bufferedSource, 1);
        if (type == Protocol.TYPE_DATA) {
            dataLength = IOUtils.peekNumberLe(bufferedSource, 1, Protocol.SIZE_DATA_BYTESIZE);
            return Protocol.TYPE_NUMBER_END;
        }
        return type;
    }

    private int pullType() throws IOException {
        int type = bufferedSource.readByte() & HEX_255;
        if (type == Protocol.TYPE_DATA) {
            dataLength = IOUtils.peekNumberLe(bufferedSource, Protocol.SIZE_DATA_BYTESIZE);
            return Protocol.TYPE_NUMBER_END;
        }
        return type;
    }

    private long pullNumber(int byteCount) throws IOException {
        return IOUtils.readNumberLe(bufferedSource, byteCount);
    }

    private SerializationException typeMismatchError(int expectedType, int actualType) {
        return new SerializationException("Expected '" +
                typeName(expectedType) +
                "', but was '" +
                typeName(actualType) +
                "'.");
    }

    private TypeToken getToken(int type) throws SerializationException {
        if (type >= Protocol.TYPE_NUMBER_START && type <= Protocol.TYPE_NUMBER_END ||
                type >= Protocol.TYPE_NUMBER_COMPRESSED_START && type <= Protocol.TYPE_NUMBER_COMPRESSED_END) {
            // Number, it may be a 1-8 byte long integer or an index for
            // number types with compression optimization
            return TypeToken.NUMBER;
        } else if ((type >= Protocol.TYPE_STRING_START && type <= Protocol.TYPE_STRING_END) ||
                (type >= Protocol.TYPE_STRING_REUSED_START && type <= Protocol.TYPE_STRING_REUSED_END) ||
                (type >= Protocol.TYPE_STRING_COMPRESSED_START && type <= Protocol.TYPE_STRING_COMPRESSED_REUSED_END)) {
            // Number, it may be a 1-8 byte long integer or an index for
            // number types with compression optimization
            return TypeToken.STRING;
        } else if (type == Protocol.TYPE_BEGIN_OBJECT) {
            // Object
            return TypeToken.BEGIN_OBJECT;
        } else if (type == Protocol.TYPE_BEGIN_ARRAY) {
            // Array
            return TypeToken.BEGIN_ARRAY;
        } else if (type >= Protocol.TYPE_BOOLEAN_FALSE && type <= Protocol.TYPE_BOOLEAN_TRUE) {
            // Boolean
            return TypeToken.BOOLEAN;
        } else if (type == Protocol.TYPE_END_ARRAY_OBJECT) {
            return currentScope == SCOPE_OBJECT ? TypeToken.END_OBJECT : TypeToken.END_ARRAY;
        } else {
            throw new SerializationException("Unknown type " + type);
        }
    }

    private String typeName(final int type) {
        TypeToken typeToken;
        switch (type) {
            case TYPE_AGGREGATE_NUMBER:
                // Number, may a 1-8 byte long integer.
                // Number, with compression optimization
                typeToken = TypeToken.NUMBER;
                break;
            case TYPE_AGGREGATE_STRING:
                // Object
                typeToken = TypeToken.STRING;
                break;
            case TYPE_AGGREGATE_BOOLEAN:
                // Boolean, 18 means 'false', 19 is 'true'
                typeToken = TypeToken.BOOLEAN;
                break;
            case TYPE_AGGREGATE_END_ARRAY:
                typeToken = TypeToken.END_ARRAY;
                break;
            case TYPE_AGGREGATE_END_OBJECT:
                typeToken = TypeToken.END_OBJECT;
                break;
            default:
                try {
                    typeToken = getToken(type);
                } catch (SerializationException e) {
                    return "(Unknown type " + type + ")";
                }
        }

        return typeToken.toString().toUpperCase(Locale.ENGLISH);
    }

    private static void checkScope(int current, int expected) {
        if (current != expected) {
            throw new IllegalStateException(
                    String.format("Expected to be called when scope is `%s`, current is `%s`.",
                            scopeName(expected),
                            scopeName(current)));
        }
    }

    private static void checkScopeIsAtLeast(int current, int expected) {
        if (current < expected) {
            throw new IllegalStateException(
                    String.format("Expected to be called when scope is at least `%s`, current is `%s`.",
                            scopeName(expected),
                            scopeName(current)));
        }
    }

    private static String scopeName(final int scope) {
        switch (scope) {
            case SCOPE_RESPONSE:
                return "SCOPE_RESPONSE";
            case SCOPE_ARRAY:
                return "SCOPE_ARRAY";
            case SCOPE_OBJECT:
                return "SCOPE_OBJECT";
            case SCOPE_DATA:
                return "SCOPE_DATA";
            case SCOPE_NONE:
                return "SCOPE_NONE";
            default:
                return "<UNKNOWN>";
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

        @Override
        public void readData(OutputStream outputStream) {
            throw new UnsupportedOperationException("Data cannot be peeked.");
        }

        @Override
        public void readData(BufferedSink sink) {
            throw new UnsupportedOperationException("Data cannot be peeked.");
        }

        @Override
        public void close() {

        }
    }
}
