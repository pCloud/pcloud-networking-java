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

import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;

import java.io.IOException;

class PeekingSource extends ForwardingSource {

    private Buffer sourceBuffer;
    private BufferedSource source;
    private long offset;
    private boolean sourceExhausted;
    private long readLimit;

    PeekingSource(BufferedSource delegate, long offset) {
        this(delegate, offset, Long.MAX_VALUE);
    }

    PeekingSource(BufferedSource delegate, long offset, long readLimit) {
        super(delegate);
        this.offset = offset;
        this.source = delegate;
        this.sourceBuffer = source.buffer();
        this.readLimit = readLimit;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        if (byteCount > readLimit) {
            byteCount = readLimit;
        }

        long bytesLeftInBuffer = sourceBuffer.size();
        if (bytesLeftInBuffer < byteCount) {
            if (bytesLeftInBuffer == 0 && sourceExhausted) {
                return -1;
            }
            sourceExhausted = source.request(offset + byteCount - bytesLeftInBuffer);
        }

        long bytesRead = Math.min(byteCount, sourceBuffer.size() - offset);
        sourceBuffer.copyTo(sink, offset, bytesRead);
        offset += bytesRead;
        return bytesRead;
    }
}
