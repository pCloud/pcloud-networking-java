package com.pcloud;

import com.pcloud.protocol.ValueReader;
import com.pcloud.protocol.streaming.ProtocolReader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public abstract class ResponseBody implements Closeable{

    public abstract ProtocolReader reader();

    public Map<String,?> toValues() throws IOException {
        return new ValueReader().readObject(reader());
    }

    public abstract long contentLength();

    public abstract ResponseData data() throws IOException;
}
