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
import okio.ByteString;
import okio.Okio;
import okio.Source;

import java.io.File;
import java.io.IOException;

import static com.pcloud.internal.ClientIOUtils.closeQuietly;

public abstract class Data {

    public abstract long contentLength();

    public abstract void writeTo(BufferedSink sink) throws IOException;

    public static Data create(final byte[] data) {
        return new Data() {

            @Override
            public long contentLength() {
                return data.length;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(data);
            }
        };
    }

    public static Data create(final ByteString data) {
        return new Data() {

            @Override
            public long contentLength() {
                return data.size();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(data);
            }
        };
    }

    public static Data create(final File file) {
        return new Data() {

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(file);
                    sink.writeAll(source);
                } finally {
                    closeQuietly(source);
                }
            }
        };
    }
}
