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

package com.pcloud.utils;


import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingTimeout;
import okio.Source;
import okio.Timeout;

import java.io.IOException;

public class ByteCountingSource implements Source {

    private volatile long bytesRead;
    private boolean closed;
    private BufferedSource source;
    private ForwardingTimeout timeout;

    public ByteCountingSource(BufferedSource source) {
        this.source = source;
        this.timeout = new ForwardingTimeout(source.timeout());
    }

    public Timeout timeout() {
        return timeout;
    }

    public long bytesRead() {
        return bytesRead;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        long read = source.read(sink, byteCount);
        if (read > 0) {
            bytesRead += read;
        }
        return read;
    }


    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            source.close();
            closed = true;
        }
    }
}
