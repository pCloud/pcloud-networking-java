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

    private Buffer source;
    private long offset;

    PeekingSource(BufferedSource delegate, long offset) {
        super(delegate);
        this.offset = offset;
        this.source = delegate.buffer();
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        // Require 'byteCount' bytes ahead of current offset.
        long bytesRead = source.request(offset + byteCount) ?
                byteCount : source.size() - offset;
        if (bytesRead > 0) {
            source.copyTo(sink, offset, bytesRead);
            offset += bytesRead;
            return bytesRead;
        } else {
            return -1;
        }
    }

}
