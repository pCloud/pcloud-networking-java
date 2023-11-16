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

package com.pcloud.networking.client;

import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ValueReader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import okio.BufferedSink;

/**
 * An interface for the response body of a network call
 *
 * @see Response
 * @see ResponseData
 * @see ProtocolReader
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class ResponseBody implements Closeable {

    /**
     * Returns the {@linkplain ProtocolReader} which is able to read this {@linkplain ResponseBody}
     *
     * @return A reference to the {@linkplain ProtocolReader} which is able to read the {@linkplain RequestBody}
     */
    public abstract ProtocolReader reader();

    /**
     * Reads the data from the {@linkplain RequestBody} and constructs a {@linkplain Map} with all the data
     *
     * @return A {@linkplain Map} with all the data from this {@linkplain ResponseBody}
     * @throws IOException on failed IO operations
     */
    public final Map<String, ?> toValues() throws IOException {
        return new ValueReader().readObject(reader());
    }

    /**
     * Returns the length of the content in this {@linkplain ResponseBody}
     *
     * @return The length of the content in this {@linkplain ResponseBody}
     */
    public abstract long contentLength();

    /**
     * Returns the {@linkplain ResponseData} for this {@linkplain ResponseBody}
     *
     * @return A reference to the {@linkplain ResponseData} for this {@linkplain ResponseBody}
     * @throws IOException on failed IO operations
     */
    public abstract ResponseData data() throws IOException;

    /**
     * Returns the source {@linkplain Endpoint} for this {@linkplain ResponseBody}
     *
     * @return non-null {@linkplain Endpoint}
     */
    public abstract Endpoint endpoint();

    /**
     * Write the body content to a {@linkplain BufferedSink}
     *
     * @param sink the receiver of the data
     * @throws IOException on failed IO operations
     * @throws IllegalStateException if the response is already partially read
     * through the exposed {@linkplain ProtocolReader}.
     */
    public abstract void writeTo(BufferedSink sink) throws IOException;

    @Override
    public String toString() {
        return String.format("(%s)->[Response]: %d bytes", endpoint(), contentLength());
    }
}
