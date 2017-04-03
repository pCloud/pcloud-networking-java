package com.pcloud.networking;

import com.pcloud.protocol.streaming.ProtocolReader;
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.util.Map;

public abstract class TypeAdapter<T> {

    public abstract T deserialize(ProtocolReader reader) throws IOException;

    public abstract void serialize(ProtocolWriter writer, T value) throws IOException;

    public Map<String, ?> toProtocolValue(T value) {
        throw new UnsupportedOperationException();
    }

    public T fromProtocolValue(Map<String, ?> value){
        throw new UnsupportedOperationException();
    }
}
