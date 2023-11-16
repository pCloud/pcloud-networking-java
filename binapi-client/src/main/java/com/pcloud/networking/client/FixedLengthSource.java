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

import com.pcloud.utils.IOUtils;
import okio.Buffer;
import okio.ForwardingTimeout;
import okio.Source;
import okio.Timeout;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

abstract class FixedLengthSource implements Source {

    private static final int DEFAULT_DISCARD_TIMEOUT_MILLIS = 300;

    private long bytesRemaining;
    private boolean closed;
    private final Source source;
    private final ForwardingTimeout timeout;
    private final int discardTimeoutMillis;

    protected FixedLengthSource(Source source, long contentLength, long discardTimeout, TimeUnit timeUnit) {
        this.source = source;
        this.timeout = new ForwardingTimeout(source.timeout());
        this.bytesRemaining = contentLength;
        this.discardTimeoutMillis = (int) timeUnit.toMillis(discardTimeout);
    }

    protected FixedLengthSource(Source source, long contentLength) {
        this(source, contentLength, DEFAULT_DISCARD_TIMEOUT_MILLIS, MILLISECONDS);
    }

    public Timeout timeout() {
        return timeout;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        if (closed) throw new IllegalStateException("closed");
        if (bytesRemaining == 0) return -1;

        long read = source.read(sink, Math.min(bytesRemaining, byteCount));
        if (read == -1) {
            scrap(false); // The server didn't supply the promised content length.
            throw new ProtocolException("unexpected end of stream");
        }

        bytesRemaining -= read;
        if (bytesRemaining == 0) {
            scrap(true);
        }
        return read;
    }


    @Override
    public synchronized void close() throws IOException {
        if (!closed && bytesRemaining != 0) {
            if (discardTimeoutMillis != 0 && !IOUtils.skipAll(this, discardTimeoutMillis, MILLISECONDS)) {
                scrap(false);
            } else {
                IOUtils.skipAll(this);
                scrap(true);
            }

            closed = true;
        }
    }

    public long bytesRemaining() {
        return bytesRemaining;
    }

    protected abstract void exhausted(boolean reuseSource);

    private void scrap(boolean reuseSource) {
        Timeout oldDelegate = timeout.delegate();
        timeout.setDelegate(Timeout.NONE);
        oldDelegate.clearDeadline();
        oldDelegate.clearTimeout();
        exhausted(reuseSource);
    }
}
