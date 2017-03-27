package com.pcloud;

import okio.BufferedSource;

import java.io.Closeable;
import java.io.InputStream;

import static com.pcloud.internal.IOUtils.closeQuietly;

public abstract class ResponseBody implements Closeable{

    public abstract BufferedSource source();

    public InputStream inputStream() {
        return source().inputStream();
    }

    public abstract long contentLength();

    @Override
    public void close() {
        closeQuietly(source());
    }

    public static ResponseBody create(final long contentLength, final BufferedSource source){
        if (source == null) {
            throw new IllegalStateException("Source argument cannot be null.");
        }

        return new ResponseBody() {
            @Override
            public long contentLength() {
                return contentLength;
            }

            @Override
            public BufferedSource source() {
                return source;
            }
        };
    }
}
