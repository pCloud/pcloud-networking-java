package com.pcloud;

import com.pcloud.internal.ClientIOUtils;
import okio.Buffer;
import okio.ForwardingTimeout;
import okio.Source;
import okio.Timeout;

import java.io.IOException;
import java.net.ProtocolException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

abstract class FixedLengthSource implements Source {

    int DISCARD_STREAM_TIMEOUT_MILLIS = 200;

    private volatile long bytesRemaining;
    private boolean closed;
    private Source source;
    private ForwardingTimeout timeout;

    FixedLengthSource(Source source, long contentLength) {
        this.source = source;
        this.timeout = new ForwardingTimeout(source.timeout());
        this.bytesRemaining = contentLength;
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

    public long bytesRemaining() {
        return bytesRemaining;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            if (bytesRemaining != 0 &&
                    !ClientIOUtils.skipAll(this, DISCARD_STREAM_TIMEOUT_MILLIS, MILLISECONDS)) {
                scrap(false);
            }

            closed = true;
        }
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
