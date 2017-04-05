package com.pcloud;

import com.pcloud.protocol.ValueWriter;
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.util.Map;

public abstract class RequestBody {
    public abstract void writeТо(ProtocolWriter writer) throws IOException;

    public static final RequestBody EMPTY = new RequestBody() {
        @Override
        public void writeТо(ProtocolWriter writer) throws IOException {
            //No op.
        }

        @Override
        public String toString() {
            return "[Empty request body]";
        }
    };

    public static RequestBody fromValues(final Map<String, ?> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values argument cannot be null.");
        }

        return new RequestBody() {
            @Override
            public void writeТо(ProtocolWriter writer) throws IOException {
                new ValueWriter().writeAll(writer, values);
            }
        };
    }
}
