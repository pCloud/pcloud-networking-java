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
import java.util.Locale;

/**
 * An exception thrown whenever something goes wrong during data serialization
 *
 * @see BytesWriter
 * @see BytesReader
 */
public class SerializationException extends IOException {

    /**
     * Create with an error message format and arguments in {@code String.format()} style
     *
     * @param message the message template
     * @param args the message arguments
     */
    public SerializationException(String message, Object... args) {
        this(String.format(Locale.US, message, args));
    }

    /**
     * Create with an error message
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method)
     */
    public SerializationException(String message) {
        super(message);
    }

    /**
     * Create with an error message and a CAUSE
     *
     * @param message The detail message (which is saved for later retrieval by the getMessage() method)
     * @param cause The cause (which is saved for later retrieval by the getCause() method).
     *              (A null value is permitted, and indicates that the cause
     *              is nonexistent or unknown.)
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
