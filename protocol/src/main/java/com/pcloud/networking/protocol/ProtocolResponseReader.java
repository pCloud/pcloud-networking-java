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

import okio.BufferedSink;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A contract for a reader which can read data from a response
 *
 * @see ProtocolReader
 * @see BytesReader
 */
@SuppressWarnings("WeakerAccess")
public interface ProtocolResponseReader extends ProtocolReader {

    /**
     * Indicates the reader has begun reading a response
     */
    int SCOPE_RESPONSE = 2;
    /**
     * Indicated the reading is at the beginning of a response's attached byte data.
     */
    int SCOPE_DATA = 1;

    /**
     * A value to indicate the data content length is unknown
     */
    int UNKNOWN_SIZE = -1;

    /**
     * Begin reading a response from the data source
     * <p>
     *
     * @return the length of the response
     * @throws IOException if the source has been exhausted prior to this operation
     */
    long beginResponse() throws IOException;

    /**
     * End reading a response from the data source
     * <p>
     *
     * @return the boolean which indicates if the response has ended
     * @throws IOException if the source has been exhausted prior to this operation
     */
    boolean endResponse() throws IOException;

    /**
     * Get the length of the data that follows the response
     * <p>
     *
     * @return a long representing the data length
     */
    long dataContentLength();

    /**
     * Read the data after a response.
     * <p>
     * Use this method to red the data after reading a data-enriched response.
     * Calling this method when the scope returned by {@linkplain #currentScope()} is not
     * {@linkplain #SCOPE_DATA} will result in a {@linkplain IllegalStateException} error.
     *
     * @param sink the non-null recipient of data
     * @throws IOException           or read or write errors.
     * @throws IllegalStateException if called in scope other than @{@linkplain #SCOPE_DATA}
     */
    void readData(BufferedSink sink) throws IOException;


    /**
     * Read the data after a response.
     * <p>
     * Use this method to red the data after reading a data-enriched response.
     * Calling this method when the scope returned by {@linkplain #currentScope()} is not
     * {@linkplain #SCOPE_DATA} will result in a {@linkplain IllegalStateException} error.
     *
     * @param outputStream the non-null recipient of data
     * @throws IOException           or read or write errors.
     * @throws IllegalStateException if called in scope other than @{@linkplain #SCOPE_DATA}
     */
    void readData(OutputStream outputStream) throws IOException;

    /**
     * {@inheritDoc}
     */
    ProtocolResponseReader newPeekingReader();
}
