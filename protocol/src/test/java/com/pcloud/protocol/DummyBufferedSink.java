package com.pcloud.protocol;

import okio.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Created by Georgi on 4/25/2017.
 */
public class DummyBufferedSink implements BufferedSink {

    private BufferedSink bufferedSinkDelegate;

    public DummyBufferedSink(BufferedSink bufferedSinkDelegate) {
        this.bufferedSinkDelegate = bufferedSinkDelegate;
    }

    public BufferedSink getDelegate() {
        return bufferedSinkDelegate;
    }

    @Override
    public Buffer buffer() {
        return bufferedSinkDelegate.buffer();
    }

    @Override
    public BufferedSink write(ByteString byteString) throws IOException {
        return bufferedSinkDelegate.write(byteString);
    }

    @Override
    public BufferedSink write(byte[] source) throws IOException {
        return bufferedSinkDelegate.write(source);
    }

    @Override
    public BufferedSink write(byte[] source, int offset, int byteCount) throws IOException {
        return bufferedSinkDelegate.write(source, offset, byteCount);
    }

    @Override
    public long writeAll(Source source) throws IOException {
        return bufferedSinkDelegate.writeAll(source);
    }

    @Override
    public BufferedSink write(Source source, long byteCount) throws IOException {
        return bufferedSinkDelegate.write(source, byteCount);
    }

    @Override
    public BufferedSink writeUtf8(String string) throws IOException {
        return bufferedSinkDelegate.writeUtf8(string);
    }

    @Override
    public BufferedSink writeUtf8(String string, int beginIndex, int endIndex) throws IOException {
        return bufferedSinkDelegate.writeUtf8(string, beginIndex, endIndex);
    }

    @Override
    public BufferedSink writeUtf8CodePoint(int codePoint) throws IOException {
        return bufferedSinkDelegate.writeUtf8CodePoint(codePoint);
    }

    @Override
    public BufferedSink writeString(String string, Charset charset) throws IOException {
        return bufferedSinkDelegate.writeString(string, charset);
    }

    @Override
    public BufferedSink writeString(String string, int beginIndex, int endIndex, Charset charset) throws IOException {
        return bufferedSinkDelegate.writeString(string, beginIndex, endIndex, charset);
    }

    @Override
    public BufferedSink writeByte(int b) throws IOException {
        return bufferedSinkDelegate.writeByte(b);
    }

    @Override
    public BufferedSink writeShort(int s) throws IOException {
        return bufferedSinkDelegate.writeShort(s);
    }

    @Override
    public BufferedSink writeShortLe(int s) throws IOException {
        return bufferedSinkDelegate.writeShortLe(s);
    }

    @Override
    public BufferedSink writeInt(int i) throws IOException {
        return bufferedSinkDelegate.writeInt(i);
    }

    @Override
    public BufferedSink writeIntLe(int i) throws IOException {
        return bufferedSinkDelegate.writeIntLe(i);
    }

    @Override
    public BufferedSink writeLong(long v) throws IOException {
        return bufferedSinkDelegate.writeLong(v);
    }

    @Override
    public BufferedSink writeLongLe(long v) throws IOException {
        return bufferedSinkDelegate.writeLongLe(v);
    }

    @Override
    public BufferedSink writeDecimalLong(long v) throws IOException {
        return bufferedSinkDelegate.writeDecimalLong(v);
    }

    @Override
    public BufferedSink writeHexadecimalUnsignedLong(long v) throws IOException {
        return bufferedSinkDelegate.writeHexadecimalUnsignedLong(v);
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        bufferedSinkDelegate.write(source, byteCount);
    }

    @Override
    public void flush() throws IOException {
        bufferedSinkDelegate.flush();
    }

    @Override
    public Timeout timeout() {
        return bufferedSinkDelegate.timeout();
    }

    @Override
    public void close() throws IOException {
        bufferedSinkDelegate.close();
    }

    @Override
    public BufferedSink emit() throws IOException {
        return bufferedSinkDelegate.emit();
    }

    @Override
    public BufferedSink emitCompleteSegments() throws IOException {
        return bufferedSinkDelegate.emitCompleteSegments();
    }

    @Override
    public OutputStream outputStream() {
        return bufferedSinkDelegate.outputStream();
    }
}
