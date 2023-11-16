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
 * A {@linkplain ProtocolWriter} which forwards calls to another.
 * <p>
 * Useful for wrapping and subclassing without inheritance.
 */
public abstract class ForwardingProtocolWriter implements ProtocolWriter {

    private final ProtocolWriter delegate;

    /**
     * Construct a new {@linkplain ForwardingProtocolWriter} instance
     * @param delegate a non-null {@linkplain ProtocolWriter} to be guarded.
     *
     * @throws IllegalArgumentException on a null {@linkplain ProtocolWriter} argument.
     */
    protected ForwardingProtocolWriter(ProtocolWriter delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("ProtocolWriter argument cannot be null.");
        }
        this.delegate = delegate;
    }

    @Override
    public ProtocolWriter writeName(String name) throws IOException {
        delegate.writeName(name);
        return this;
    }

    @Override
    public ProtocolWriter writeValue(Object value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolWriter writeValue(String value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolWriter writeValue(double value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolWriter writeValue(float value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolWriter writeValue(long value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public ProtocolWriter writeValue(boolean value) throws IOException {
        delegate.writeValue(value);
        return this;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
