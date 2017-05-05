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

import com.pcloud.protocol.DataSource;

import java.io.IOException;

/**
 * A contract for an object which can write data into a data sink
 * <p>
 * Generally used to construct network requests
 *
 * @see BytesWriter
 */
public interface ProtocolRequestWriter extends ProtocolWriter {

    /**
     * Begin writing a request
     * <p>
     *
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolRequestWriter beginRequest() throws IOException;

    /**
     * Write an entire {@linkplain DataSource} into a data sink
     * <p>
     *
     * @param source the source to be written
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolRequestWriter writeData(DataSource source) throws IOException;

    /**
     * Write the method name for the request
     * <p>
     *
     * @param name A string for the name of the method this request will hit
     * @return a reference to this object
     * @throws IOException on failed IO operations
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
     * End writing a request
     * <p>
     *
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolRequestWriter endRequest() throws IOException;
}
