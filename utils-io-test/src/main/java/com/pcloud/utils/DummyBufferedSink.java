/*
 * Copyright (c) 2017 pCloud AG
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

package com.pcloud.utils;

import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;
import okio.Source;
import okio.Timeout;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DummyBufferedSink implements BufferedSink {

    private BufferedSink delegate;

    public DummyBufferedSink() {
        this(new Buffer());
    }

    public DummyBufferedSink(BufferedSink delegate) {
        this.delegate = delegate;
    }

    public BufferedSink getDelegate() {
        return delegate;
    }

    @Override
    public Buffer buffer() {
        return delegate.buffer();
    }

    @Override
    public BufferedSink write(ByteString byteString) throws IOException {
        return delegate.write(byteString);
    }

    @Override
    public BufferedSink write(byte[] source) throws IOException {
        return delegate.write(source);
    }

    @Override
    public BufferedSink write(byte[] source, int offset, int byteCount) throws IOException {
        return delegate.write(source, offset, byteCount);
    }

    @Override
    public long writeAll(Source source) throws IOException {
        return delegate.writeAll(source);
    }

    @Override
    public BufferedSink write(Source source, long byteCount) throws IOException {
        return delegate.write(source, byteCount);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    @Override
    public BufferedSink writeUtf8(String string) throws IOException {
        return delegate.writeUtf8(string);
    }

    @Override
    public BufferedSink writeUtf8(String string, int beginIndex, int endIndex) throws IOException {
        return delegate.writeUtf8(string, beginIndex, endIndex);
    }

    @Override
    public BufferedSink writeUtf8CodePoint(int codePoint) throws IOException {
        return delegate.writeUtf8CodePoint(codePoint);
    }

    @Override
    public BufferedSink writeString(String string, Charset charset) throws IOException {
        return delegate.writeString(string, charset);
    }

    @Override
    public BufferedSink writeString(String string, int beginIndex, int endIndex, Charset charset) throws IOException {
        return delegate.writeString(string, beginIndex, endIndex, charset);
    }

    @Override
    public BufferedSink writeByte(int b) throws IOException {
        return delegate.writeByte(b);
    }

    @Override
    public BufferedSink writeShort(int s) throws IOException {
        return delegate.writeShort(s);
    }

    @Override
    public BufferedSink writeShortLe(int s) throws IOException {
        return delegate.writeShortLe(s);
    }

    @Override
    public BufferedSink writeInt(int i) throws IOException {
        return delegate.writeInt(i);
    }

    @Override
    public BufferedSink writeIntLe(int i) throws IOException {
        return delegate.writeIntLe(i);
    }

    @Override
    public BufferedSink writeLong(long v) throws IOException {
        return delegate.writeLong(v);
    }

    @Override
    public BufferedSink writeLongLe(long v) throws IOException {
        return delegate.writeLongLe(v);
    }

    @Override
    public BufferedSink writeDecimalLong(long v) throws IOException {
        return delegate.writeDecimalLong(v);
    }

    @Override
    public BufferedSink writeHexadecimalUnsignedLong(long v) throws IOException {
        return delegate.writeHexadecimalUnsignedLong(v);
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        delegate.write(source, byteCount);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public Timeout timeout() {
        return delegate.timeout();
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public BufferedSink emit() throws IOException {
        return delegate.emit();
    }

    @Override
    public BufferedSink emitCompleteSegments() throws IOException {
        return delegate.emitCompleteSegments();
    }

    @Override
    public OutputStream outputStream() {
        return delegate.outputStream();
    }
}
