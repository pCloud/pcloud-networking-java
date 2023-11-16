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

package com.pcloud.networking.client;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import static com.pcloud.utils.IOUtils.closeQuietly;

/**
 * An implementation to represent the data for a network call {@linkplain ResponseBody}
 *
 * @see Response
 * @see ResponseBody
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ResponseData implements Closeable {

    private final BufferedSource source;
    private final long contentLength;

    ResponseData(BufferedSource source, long contentLength) {
        this.source = source;
        this.contentLength = contentLength;
    }

    /**
     * Returns an {@linkplain InputStream} to read the data from the source
     *
     * @return An {@linkplain InputStream} to read the data from the source
     */
    public InputStream byteStream() {
        //noinspection resource
        return source().inputStream();
    }

    /**
     * A {@linkplain BufferedSource} which is the source of the data
     *
     * @return A reference to the source of the data
     */
    public BufferedSource source() {
        return source;
    }

    /**
     * Reads and returns the content of the data as a {@linkplain ByteString} object
     *
     * @return The content of the source as a {@linkplain ByteString}
     * @throws IOException on failed IO operations
     */
    public ByteString byteString() throws IOException {
        try {
            return source.readByteString(contentLength);
        } finally {
            close();
        }
    }

    /**
     * Returns the data from the source as a byte array
     *
     * @return The data from the source as a byte array
     * @throws IOException on failed IO operations
     */
    public byte[] bytes() throws IOException {
        try {
            return source.readByteArray(contentLength);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        closeQuietly(source);
    }

    /**
     * Returns the length of the content in the source
     *
     * @return The length of the content in the source
     */
    public long contentLength() {
        return contentLength;
    }

    /**
     * Writes all the data from the source into a {@linkplain BufferedSink}
     *
     * @param sink The sink for the data to be written to
     * @throws IOException on failed IO operations
     */
    public void writeTo(BufferedSink sink) throws IOException {
        try {
            sink.writeAll(source);
        } finally {
            close();
        }
    }
}
