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
 * A {@linkplain ProtocolRequestWriter} which forwards calls to another.
 * <p>
 * Useful for wrapping and subclassing without inheritance.
 */
public abstract class ForwardingProtocolRequestWriter implements ProtocolRequestWriter {

    private final ProtocolRequestWriter delegate;

    /**
     * Construct a new {@linkplain ForwardingProtocolRequestWriter} instance
     * @param delegate a non-null {@linkplain ProtocolRequestWriter} to be guarded.
     *
     * @throws IllegalArgumentException on a null {@linkplain ProtocolRequestWriter} argument.
     */
    protected ForwardingProtocolRequestWriter(ProtocolRequestWriter delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("ProtocolRequestWriter argument cannot be null.");
        }
        this.delegate = delegate;
    }

    @Override
    public ProtocolRequestWriter beginRequest() throws IOException {
        delegate.beginRequest();
        return this;
    }

    @Override
    public ProtocolRequestWriter writeData(DataSource source) throws IOException {
        delegate.writeData(source);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeMethodName(String name) throws IOException {
        delegate.writeMethodName(name);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeName(String name) throws IOException {
        delegate.writeName(name);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(Object value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(String value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(double value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(float value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(long value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolRequestWriter writeValue(boolean value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public ProtocolRequestWriter endRequest() throws IOException {
        delegate.endRequest();
        return this;
    }
}
