package com.pcloud;

import com.pcloud.value.ObjectValue;
import com.pcloud.value.Value;
import okio.BufferedSink;

import java.io.IOException;
import java.util.Map;

public abstract class RequestBody {
    public abstract void writeAll(BufferedSink sink) throws IOException;

    public static final RequestBody EMPTY = new RequestBody() {
        @Override
        public void writeAll(BufferedSink sink) throws IOException {
            sink.writeByte(0);
        }

        @Override
        public String toString() {
            return "[Empty request body]";
        }
    };

    public static RequestBody fromValues(final Map<String, Object> requestParameters){
        if (requestParameters == null) {
            throw new IllegalArgumentException("Value object cannot be null.");
        }

        return new RequestBody() {
            @Override
            public void writeAll(BufferedSink sink) throws IOException {
                BinaryProtocolCodec.writeValues(sink, requestParameters);
            }

            @Override
            public String toString() {
                return requestParameters.toString();
            }
        };
    }

    public static RequestBody fromValues(final ObjectValue requestParameters){
        if (requestParameters == null) {
            throw new IllegalArgumentException("Value object cannot be null.");
        }

        return new RequestBody() {
            @Override
            public void writeAll(BufferedSink sink) throws IOException {
                BinaryProtocolCodec.writeValues(sink, requestParameters);
            }

            @Override
            public String toString() {
                return requestParameters.toString();
            }
        };
    }
}
