/*
 * Copyright (C) 2017 pCloud AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.protocol.streaming;

import com.pcloud.protocol.DataSource;
import okio.BufferedSource;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public interface ProtocolWriter extends AutoCloseable, Closeable, Flushable {
    ProtocolWriter beginRequest() throws IOException;

    ProtocolWriter method(String name) throws IOException;

    ProtocolWriter endRequest() throws IOException;

    ProtocolWriter data(DataSource source) throws IOException;

    ProtocolWriter parameter(String name, Object value) throws IOException;

    ProtocolWriter parameter(String name, String value) throws IOException;

    ProtocolWriter parameter(String name, long value) throws IOException;

    ProtocolWriter parameter(String name, boolean value) throws IOException;

    @Override
    void close();

    @Override
    void flush() throws IOException;
}
