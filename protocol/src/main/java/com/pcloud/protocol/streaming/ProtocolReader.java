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

import com.pcloud.protocol.DataSink;

import java.io.Closeable;
import java.io.IOException;

public interface ProtocolReader extends AutoCloseable, Closeable {
    TypeToken peek() throws IOException;

    long beginResponse() throws IOException;

    long endResponse() throws IOException;

    void beginObject() throws IOException;

    void beginArray() throws IOException;

    void endArray() throws IOException;

    void endObject() throws IOException;

    boolean readBoolean() throws IOException;

    String readString() throws IOException;

    long readNumber() throws IOException;

    @Override
    void close();

    boolean hasNext() throws IOException;

    long dataContentLength();

    void skipValue() throws IOException;
}
