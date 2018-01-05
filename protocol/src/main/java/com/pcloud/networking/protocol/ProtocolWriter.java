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
 * A contract for a encoding parameters and their values in a type-safe manner
 *
 * @see BytesWriter
 * @see ValueWriter
 */
public interface ProtocolWriter extends AutoCloseable, Closeable {

    /**
     * Write the name of a parameter
     * <p>
     * Every call of this method should be followed by a call to one the variants of {@code writeValue()}
     *
     * @param name the non-null name of the parameter
     * @return a reference to this object
     * @throws IOException              on a failed IO operation
     * @throws IllegalArgumentException on a null argument
     */
    ProtocolWriter writeName(String name) throws IOException;

    /**
     * Write the value of a parameter
     * <p>
     * Every call to this or the other variants of {@code writeValue()} should be preceded by a call to
     * {@linkplain #writeName(String)}.
     * <p>
     * The provided argument must be a non-null object, of type {@link String} or any of the boxed variants of
     * primitive types. An object will be accepted if {@linkplain Class#isPrimitive()} returns true for the object type.
     *
     * @param value a non-null object
     * @return a reference to this object
     * @throws IOException              on a failed IO operation
     * @throws IllegalArgumentException on a null argument
     * @throws IllegalArgumentException if the argument is of invalid type
     */
    ProtocolWriter writeValue(Object value) throws IOException;

    /**
     * Write the value of a parameter
     * <p>
     * Every call to this or the other variants of {@code writeValue()} should be preceded by a call to
     * {@linkplain #writeName(String)}.
     *
     * @param value a non-null String object
     * @return a reference to this object
     * @throws IOException              on failed IO operations
     * @throws IllegalArgumentException on a null argument
     */
    ProtocolWriter writeValue(String value) throws IOException;

    /**
     * Write the value of a parameter
     * <p>
     * Every call to this or the other variants of {@code writeValue()} should be preceded by a call to
     * {@linkplain #writeName(String)}.
     *
     * @param value a double
     * @return a reference to this object
     * @throws IOException on a failed IO operation
     */
    ProtocolWriter writeValue(double value) throws IOException;

    /**
     * Write the value of a parameter
     * <p>
     * Every call to this or the other variants of {@code writeValue()} should be preceded by a call to
     * {@linkplain #writeName(String)}.
     *
     * @param value a float
     * @return a reference to this object
     * @throws IOException on a failed IO operation
     */
    ProtocolWriter writeValue(float value) throws IOException;

    /**
     * Write the value of a parameter
     * <p>
     * Every call to this or the other variants of {@code writeValue()} should be preceded by a call to
     * {@linkplain #writeName(String)}.
     *
     * @param value a long
     * @return a reference to this object
     * @throws IOException on a failed IO operation
     */
    ProtocolWriter writeValue(long value) throws IOException;

    /**
     * Write the value of a parameter
     * <p>
     * Every call to this or the other variants of {@code writeValue()} should be preceded by a call to
     * {@linkplain #writeName(String)}.
     *
     * @param value a boolean
     * @return a reference to this object
     * @throws IOException on a failed IO operation
     */
    ProtocolWriter writeValue(boolean value) throws IOException;

    @Override
    void close();
}
