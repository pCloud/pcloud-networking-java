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

import com.pcloud.networking.protocol.ValueWriter;
import com.pcloud.networking.protocol.ProtocolWriter;

import java.io.IOException;
import java.util.Map;

/**
 * An interface to represent a request body for a {@linkplain Request}
 *
 * @see Request
 * @see ProtocolWriter
 */
public abstract class RequestBody {
    /**
     * Writes data to a {@linkplain ProtocolWriter}
     *
     * @param writer The {@linkplain ProtocolWriter} to write the data
     * @throws IOException on failed IO operations
     */
    public abstract void writeTo(ProtocolWriter writer) throws IOException;

    /**
     * Used to construct an empty {@linkplain RequestBody} when there is no need to write any data to it
     */
    public static final RequestBody EMPTY = new RequestBody() {
        @Override
        public void writeTo(ProtocolWriter writer) throws IOException {
            //No op.
        }

        @Override
        public String toString() {
            return "[Empty request body]";
        }
    };

    /**
     * Constructs a {@linkplain RequestBody} with data from a {@linkplain Map}
     *
     * @param values A {@linkplain Map} containing all the parameters to contruct a {@linkplain RequestBody}
     * @return A reference to a new {@linkplain RequestBody} with all the data from the {@linkplain Map} argument written to it
     * @throws IllegalArgumentException on a null {@linkplain Map} argument
     */
    public static RequestBody fromValues(final Map<String, ?> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values argument cannot be null.");
        }

        return new RequestBody() {
            @Override
            public void writeTo(ProtocolWriter writer) throws IOException {
                new ValueWriter().writeAll(writer, values);
            }
        };
    }
}
