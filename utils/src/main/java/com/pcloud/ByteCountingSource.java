package com.pcloud;

import okio.*;

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
