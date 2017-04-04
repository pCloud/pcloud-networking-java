package com.pcloud.protocol.streaming;

import java.io.IOException;

public class SerializationException extends IOException {

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
