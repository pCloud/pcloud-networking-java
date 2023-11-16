/*
 * Copyright (c) 2018 pCloud AG
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

package com.pcloud.networking.protocol;

import okio.BufferedSink;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@linkplain ProtocolResponseReader} which forwards calls to another.
 * <p>
 * Useful for wrapping and subclassing without inheritance.
 */
public abstract class ForwardingProtocolResponseReader implements ProtocolResponseReader {

    private final ProtocolResponseReader delegate;

    /**
     * Construct a new {@linkplain ForwardingProtocolResponseReader} instance
     * @param delegate a non-null {@linkplain ProtocolResponseReader} to be guarded.
     *
     * @throws IllegalArgumentException on a null {@linkplain ProtocolResponseReader} argument.
     */
    protected ForwardingProtocolResponseReader(ProtocolResponseReader delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("ProtocolResponseReader argument cannot be null.");
        }
        this.delegate = delegate;
    }

    @Override
    public long beginResponse() throws IOException {
        return delegate.beginResponse();
    }

    @Override
    public boolean endResponse() throws IOException {
        return delegate.endResponse();
    }

    @Override
    public long dataContentLength() {
        return delegate.dataContentLength();
    }

    @Override
    public void readData(BufferedSink sink) throws IOException {
        delegate.readData(sink);
    }

    @Override
    public void readData(OutputStream outputStream) throws IOException {
        delegate.readData(outputStream);
    }

    @Override
    public int currentScope() {
        return delegate.currentScope();
    }

    @Override
    public TypeToken peek() throws IOException {
        return delegate.peek();
    }

    @Override
    public void beginObject() throws IOException {
        delegate.beginObject();
    }

    @Override
    public void beginArray() throws IOException {
        delegate.beginArray();
    }

    @Override
    public void endArray() throws IOException {
        delegate.endArray();
    }

    @Override
    public void endObject() throws IOException {
        delegate.endObject();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return delegate.readBoolean();
    }

    @Override
    public String readString() throws IOException {
        return delegate.readString();
    }

    @Override
    public long readNumber() throws IOException {
        return delegate.readNumber();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean hasNext() throws IOException {
        return delegate.hasNext();
    }

    @Override
    public void skipValue() throws IOException {
        delegate.skipValue();
    }

    @Override
    public ProtocolResponseReader newPeekingReader() {
        return delegate.newPeekingReader();
    }
}
