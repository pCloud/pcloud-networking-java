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

import java.io.IOException;

/**
 * A contract for serializing requests
 *
 * @see BytesWriter
 */
public interface ProtocolRequestWriter extends ProtocolWriter {

    /**
     * Start a request
     * <p>
     *
     * @return a reference to this object
     * @throws IOException on a failed IO operation
     */
    ProtocolRequestWriter beginRequest() throws IOException;

    /**
     * Set a {@linkplain DataSource} as request data
     * <p>
     *
     * @param source a non-null {@linkplain DataSource} object
     * @return a reference to this object
     * @throws IOException              on a failed IO operation
     * @throws IllegalArgumentException on a null argument
     */
    ProtocolRequestWriter writeData(DataSource source) throws IOException;

    /**
     * Write the method name of the request
     * <p>
     *
     * @param name a non-null string
     * @return a reference to this object
     * @throws IOException              on a failed IO operation
     * @throws IllegalArgumentException on a null argument
     */
    ProtocolRequestWriter writeMethodName(String name) throws IOException;

    @Override
    ProtocolRequestWriter writeName(String name) throws IOException;

    @Override
    ProtocolRequestWriter writeValue(Object value) throws IOException;

    @Override
    ProtocolRequestWriter writeValue(String value) throws IOException;

    @Override
    ProtocolRequestWriter writeValue(double value) throws IOException;

    @Override
    ProtocolRequestWriter writeValue(float value) throws IOException;

    @Override
    ProtocolRequestWriter writeValue(long value) throws IOException;

    @Override
    ProtocolRequestWriter writeValue(boolean value) throws IOException;

    /**
     * Finish a request
     *
     * @return a reference to this object
     * @throws IOException on a failed IO operation
     */
    ProtocolRequestWriter endRequest() throws IOException;
}
