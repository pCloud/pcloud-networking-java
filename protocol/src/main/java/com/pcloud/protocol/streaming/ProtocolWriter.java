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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public interface ProtocolWriter extends AutoCloseable, Closeable, Flushable {

    ProtocolWriter writeName(String name, TypeToken type) throws IOException;

    ProtocolWriter writeValue(Object value) throws IOException;

    ProtocolWriter writeValue(String value) throws IOException;

    ProtocolWriter writeValue(double value) throws IOException;

    ProtocolWriter writeValue(float value) throws IOException;

    ProtocolWriter writeValue(long value) throws IOException;

    ProtocolWriter writeValue(boolean value) throws IOException;

    @Override
    void close();

    @Override
    void flush() throws IOException;
}
