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

import java.io.IOException;

/**
 * A {@linkplain ProtocolReader} which forwards calls to another.
 * <p>
 * Useful for wrapping and subclassing without inheritance.
 */
public abstract class ForwardingProtocolReader implements ProtocolReader {

    private ProtocolReader delegate;

    /**
     * Construct a new {@linkplain ForwardingProtocolReader} instance
     * @param delegate a non-null {@linkplain ProtocolReader} to be guarded.
     *
     * @throws IllegalArgumentException on a null {@linkplain ProtocolReader} argument.
     */
    protected ForwardingProtocolReader(ProtocolReader delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("ProtocolReader argument cannot be null.");
        }
        this.delegate = delegate;
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
    public ProtocolReader newPeekingReader() {
        return delegate.newPeekingReader();
    }
}
