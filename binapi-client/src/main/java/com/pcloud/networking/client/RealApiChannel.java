package com.pcloud.networking.client;

import com.pcloud.networking.protocol.BytesReader;
import com.pcloud.networking.protocol.BytesWriter;
import com.pcloud.networking.protocol.DataSource;
import com.pcloud.networking.protocol.ForwardingProtocolRequestWriter;
import com.pcloud.networking.protocol.ForwardingProtocolResponseReader;
import com.pcloud.networking.protocol.ProtocolRequestWriter;
import com.pcloud.networking.protocol.ProtocolResponseReader;
import com.pcloud.networking.protocol.TypeToken;
import okio.BufferedSink;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;

class RealApiChannel implements ApiChannel {

    private ConnectionProvider connectionProvider;
    private Connection connection;
    private ProtocolRequestWriter writer;
    private ProtocolResponseReader reader;
    private final Endpoint endpoint;
    private volatile long completedRequests;
    private volatile long completedResponses;
    private volatile boolean closed;

    private final Object counterLock = new Object();

    RealApiChannel(ConnectionProvider connectionProvider, Endpoint endpoint) throws IOException {
        this.connectionProvider = connectionProvider;
        this.connection = connectionProvider.obtainConnection(endpoint);
        this.endpoint = connection.endpoint();
        this.writer = new CountingProtocolRequestWriter(new BytesWriter(connection.sink()), this);
        this.reader = new CountingProtocolResponseReader(new BytesReader(connection.source()), this);
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public ProtocolResponseReader reader() {
        return reader;
    }

    @Override
    public ProtocolRequestWriter writer() {
        return writer;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public boolean isIdle() {
        synchronized (counterLock) {
            return completedRequests == completedResponses;
        }
    }

    @Override
    public void close() {
        if (!closed) {
            // Lock to avoid recycling the same connection twice
            // by competing threads.
            synchronized (this) {
                if (!closed) {
                    closed = true;
                    if (isIdle()) {
                        connectionProvider.recycleConnection(connection);
                    } else {
                        connection.close();
                    }
                    connection = null;
                }
            }
        }
    }

    private void checkNotClosed() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    private void completeResponse() {
        synchronized (counterLock) {
            completedResponses++;
        }
    }

    private void completeRequest() {
        synchronized (counterLock) {
            completedRequests++;
        }
    }

    private static class CountingProtocolRequestWriter extends ForwardingProtocolRequestWriter {

        private RealApiChannel apiChannel;

        CountingProtocolRequestWriter(ProtocolRequestWriter delegate, RealApiChannel apiChannel) {
            super(delegate);
            this.apiChannel = apiChannel;
        }

        @Override
        public ProtocolRequestWriter beginRequest() throws IOException {
            apiChannel.checkNotClosed();
            super.beginRequest();
            return this;
        }

        @Override
        public ProtocolRequestWriter writeData(DataSource source) throws IOException {
            apiChannel.checkNotClosed();
            super.writeData(source);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeMethodName(String name) throws IOException {
            apiChannel.checkNotClosed();
            super.writeMethodName(name);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeName(String name) throws IOException {
            apiChannel.checkNotClosed();
            super.writeName(name);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeValue(Object value) throws IOException {
            apiChannel.checkNotClosed();
            super.writeValue(value);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeValue(String value) throws IOException {
            apiChannel.checkNotClosed();
            super.writeValue(value);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeValue(double value) throws IOException {
            apiChannel.checkNotClosed();
            super.writeValue(value);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeValue(float value) throws IOException {
            apiChannel.checkNotClosed();
            super.writeValue(value);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeValue(long value) throws IOException {
            apiChannel.checkNotClosed();
            super.writeValue(value);
            return this;
        }

        @Override
        public ProtocolRequestWriter writeValue(boolean value) throws IOException {
            apiChannel.checkNotClosed();
            super.writeValue(value);
            return this;
        }

        @Override
        public void close() {
            apiChannel.close();
        }

        @Override
        public ProtocolRequestWriter endRequest() throws IOException {
            apiChannel.checkNotClosed();
            // Mark request as complete before completing it to avoid
            // race conditions between closing the channel and completing the actual write
            // to the underlying sink. By marking in advance the case in question materializes,
            // the channel's connection will not be reused without being sure it's clean and
            // there aren't any unfinished request/response pairs.
            apiChannel.completeRequest();
            super.endRequest();
            return this;
        }
    }

    private static class CountingProtocolResponseReader extends ForwardingProtocolResponseReader {

        private RealApiChannel apiChannel;

        CountingProtocolResponseReader(ProtocolResponseReader delegate, RealApiChannel apiChannel) {
            super(delegate);
            this.apiChannel = apiChannel;
        }

        @Override
        public long beginResponse() throws IOException {
            apiChannel.checkNotClosed();
            return super.beginResponse();
        }

        @Override
        public boolean endResponse() throws IOException {
            apiChannel.checkNotClosed();
            boolean hasData = super.endResponse();
            if (!hasData) {
                apiChannel.completeResponse();
            }
            return hasData;
        }

        @Override
        public void readData(BufferedSink sink) throws IOException {
            apiChannel.checkNotClosed();
            super.readData(sink);
            apiChannel.completeResponse();
        }

        @Override
        public void readData(OutputStream outputStream) throws IOException {
            apiChannel.checkNotClosed();
            super.readData(outputStream);
            apiChannel.completeResponse();
        }

        @Override
        public TypeToken peek() throws IOException {
            apiChannel.checkNotClosed();
            return super.peek();
        }

        @Override
        public void beginObject() throws IOException {
            apiChannel.checkNotClosed();
            super.beginObject();
        }

        @Override
        public void beginArray() throws IOException {
            apiChannel.checkNotClosed();
            super.beginArray();
        }

        @Override
        public void endArray() throws IOException {
            apiChannel.checkNotClosed();
            super.endArray();
        }

        @Override
        public void endObject() throws IOException {
            apiChannel.checkNotClosed();
            super.endObject();
        }

        @Override
        public boolean readBoolean() throws IOException {
            apiChannel.checkNotClosed();
            return super.readBoolean();
        }

        @Override
        public String readString() throws IOException {
            apiChannel.checkNotClosed();
            return super.readString();
        }

        @Override
        public long readNumber() throws IOException {
            apiChannel.checkNotClosed();
            return super.readNumber();
        }

        @Override
        public void close() {
            // Let the ApiChannel decide how to release held resources.
            apiChannel.close();
        }

        @Override
        public boolean hasNext() throws IOException {
            apiChannel.checkNotClosed();
            return super.hasNext();
        }

        @Override
        public void skipValue() throws IOException {
            apiChannel.checkNotClosed();
            super.skipValue();
        }

        @Override
        public ProtocolResponseReader newPeekingReader() {
            return new CountingProtocolResponseReader(super.newPeekingReader(), apiChannel);
        }
    }
}
