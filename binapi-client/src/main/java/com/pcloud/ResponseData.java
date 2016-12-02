/*
 * Copyright (c) 2016 Georgi Neykov
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

package com.pcloud;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import static com.pcloud.IOUtils.closeQuietly;

public class ResponseData extends Data implements Closeable{

    private final BufferedSource source;
    private final long contentLength;


    public ResponseData(BufferedSource source, long contentLength) {
        this.source = source;
        this.contentLength = contentLength;
    }

    public InputStream byteStream() {
        return source().inputStream();
    }

    public BufferedSource source() {
        return source;
    }

    public ByteString byteString() throws IOException {
        try {
            return source.readByteString(contentLength());
        } finally {
            close();
        }
    }

    public byte[] bytes() throws IOException {
        try {
            return source.readByteArray(contentLength());
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        closeQuietly(source);
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        try {
            sink.writeAll(source);
        } finally {
            close();
        }
    }
}
