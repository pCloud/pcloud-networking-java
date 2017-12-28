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

import java.io.Closeable;
import java.io.IOException;

/**
 * A contract for a writer object which can write data into a data sink
 * @see BytesWriter
 * @see ValueWriter
 */
public interface ProtocolWriter extends AutoCloseable, Closeable {

    /**
     * Write a key for a data pair
     * <p>
     * The data is written in key - value pairs so every call of this
     * method should be followed by one of the writeValue overloads.
     * <p>
     *
     * @param name the key for this data pair
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolWriter writeName(String name) throws IOException;

    /**
     * Write a value for a data pair
     * <p>
     * Every call to this or the other overloads should be preceded by a call to {@linkplain #writeName(String)}
     *
     * @param value an object to be written in the data sink
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolWriter writeValue(Object value) throws IOException;

    /**
     * Write a value for a data pair
     * <p>
     * Every call to this or the other overloads should be preceded by a call to {@linkplain #writeName(String)}
     *
     * @param value a String to be written in the data sink
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolWriter writeValue(String value) throws IOException;

    /**
     * Write a value for a data pair
     * <p>
     * Every call to this or the other overloads should be preceded by a call to {@linkplain #writeName(String)}
     *
     * @param value a double to be written in the data sink
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolWriter writeValue(double value) throws IOException;

    /**
     * Write a value for a data pair
     * <p>
     * Every call to this or the other overloads should be preceded by a call to {@linkplain #writeName(String)}
     *
     * @param value a float to be written in the data sink
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolWriter writeValue(float value) throws IOException;

    /**
     * Write a value for a data pair
     * <p>
     * Every call to this or the other overloads should be preceded by a call to {@linkplain #writeName(String)}
     *
     * @param value a long to be written in the data sink
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolWriter writeValue(long value) throws IOException;

    /**
     * Write a value for a data pair
     * <p>
     * Every call to this or the other overloads should be preceded by a call to {@linkplain #writeName(String)}
     *
     * @param value a boolean to be written in the data sink
     * @return a reference to this object
     * @throws IOException on failed IO operations
     */
    ProtocolWriter writeValue(boolean value) throws IOException;

    /**
     *  Close the data source
     */
    @Override
    void close();
}
