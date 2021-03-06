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

import com.pcloud.networking.protocol.BytesWriter;
import com.pcloud.networking.protocol.ProtocolRequestWriter;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;

import java.io.IOException;

public class RequestBytesWriter {

    public void writeTo(BufferedSink sink, Request request) throws IOException {
        ProtocolRequestWriter writer = new BytesWriter(sink);
        writer.beginRequest()
                .writeMethodName(request.methodName());
        request.body().writeTo(writer);
        if (request.dataSource() != null) {
            writer.writeData(request.dataSource());
        }
        writer.endRequest();
    }

    public ByteString bytes(Request request) throws IOException {
        Buffer buffer = new Buffer();
        this.writeTo(buffer, request);
        return buffer.readByteString();
    }

}
