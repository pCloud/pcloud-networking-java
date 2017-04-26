/*
 * Copyright (C) 2017 pCloud AG.
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

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Inherited;

/**
 * A contract for an object capable of reading serialized data from a data source.
 *
 * @see BytesReader
 * @see com.pcloud.protocol.ValueReader
 */
public interface ProtocolReader extends AutoCloseable, Closeable {

    /**
     * Indicates the reader has either not begun reading or has finished reading all the data
     * and is currently not reading anything
     */
    int SCOPE_NONE = -1;

    /**
     * Indicates the reader is currently reading an object from the data source
     */
    int SCOPE_OBJECT = 2;

    /**
     * Indicates the reader is currently reading an array from the data source
     */
    int SCOPE_ARRAY = 3;

    /**
     * Get the current scope of the reader
     * <p>
     * Return values can be {@linkplain ProtocolReader#SCOPE_NONE}, {@linkplain ProtocolReader#SCOPE_OBJECT},
     * {@linkplain ProtocolReader#SCOPE_ARRAY}, {@linkplain ProtocolResponseReader#SCOPE_RESPONSE}
     *
     * @return a constant integer representing the current scope of the reader
     */
    int currentScope();

    /**
     * Peek the data and check what is the type of the data that follows
     * <p>
     * Return values can be {@linkplain TypeToken#BEGIN_OBJECT}, {@linkplain TypeToken#BEGIN_ARRAY},
     * {@linkplain TypeToken#END_OBJECT}, {@linkplain TypeToken#END_ARRAY}, {@linkplain TypeToken#STRING},
     * {@linkplain TypeToken#BOOLEAN}, {@linkplain TypeToken#NUMBER}
     *
     * @return A {@linkplain TypeToken} constant representing the type of data that follows
     * @throws IOException if the source is exhausted before the next bytes can be read
     * @see TypeToken
     */
    TypeToken peek() throws IOException;

    /**
     * Begin reading an object from the source
     * <p>
     *
     * @throws IOException if the source has been exhausted
     */
    void beginObject() throws IOException;

    /**
     * Begin reading an array from the source
     * <p>
     *
     * @throws IOException if the source has been exhausted
     */
    void beginArray() throws IOException;

    /**
     * End reading an array from the source
     * <p>
     *
     * @throws IOException if the source has been exhausted
     */
    void endArray() throws IOException;

    /**
     * End reading an object from the source
     * <p>
     *
     * @throws IOException if the source has been exhausted
     */
    void endObject() throws IOException;

    /**
     * Read a boolean value from the source
     * <p>
     *
     * @return the value of the boolean being read
     * @throws IOException if the source has been exhausted
     */
    boolean readBoolean() throws IOException;

    /**
     * Read a String from the source
     * <p>
     *
     * @return the String being read
     * @throws IOException if the source is exhausted
     */
    String readString() throws IOException;

    /**
     * Read a number from the source
     * <p>
     *
     * @return a long with the value of the number being read
     * @throws IOException if the source is exhausted
     */
    long readNumber() throws IOException;

    /**
     *  Close the data source
     */
    @Override
    void close();

    /**
     * Check if the source has more data to read
     * <p>
     *
     * @return true if there are bytes to read, false otherwise
     * @throws IOException if the source has been exhausted
     */
    boolean hasNext() throws IOException;

    /**
     * Consume and discard the next value
     * <p>
     *
     * @throws IOException if the source has been exhausted
     */
    void skipValue() throws IOException;

    /**
     * Get a new instance peeking instance of this reader.
     * <p>
     * The peeking reader can read data without consuming it.
     * It inherits all the fields of the current instance such as the data source and the scope.
     * Any operations with the peeking reader will not exhaust the data source.
     *
     * @return a new instance of a peeking ProtocolReader
     */
    ProtocolReader newPeekingReader();
}
