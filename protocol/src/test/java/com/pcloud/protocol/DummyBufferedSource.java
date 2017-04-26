package com.pcloud.protocol;

import okio.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by Georgi on 4/25/2017.
 */
public class DummyBufferedSource implements BufferedSource {

    private BufferedSource sourceDelegate;

    DummyBufferedSource(BufferedSource sourceDelegate) {

        this.sourceDelegate = sourceDelegate;
    }

    public BufferedSource getSourceDelegate() {
        return sourceDelegate;
    }

    @Override
    public Buffer buffer() {
        return sourceDelegate.buffer();
    }

    @Override
    public boolean exhausted() throws IOException {
        return sourceDelegate.exhausted();
    }

    @Override
    public void require(long byteCount) throws IOException {
        sourceDelegate.require(byteCount);
    }

    @Override
    public boolean request(long byteCount) throws IOException {
        return sourceDelegate.request(byteCount);
    }

    @Override
    public byte readByte() throws IOException {
        return sourceDelegate.readByte();
    }

    @Override
    public short readShort() throws IOException {
        return sourceDelegate.readShort();
    }

    @Override
    public short readShortLe() throws IOException {
        return sourceDelegate.readShortLe();
    }

    @Override
    public int readInt() throws IOException {
        return sourceDelegate.readInt();
    }

    @Override
    public int readIntLe() throws IOException {
        return sourceDelegate.readIntLe();
    }

    @Override
    public long readLong() throws IOException {
        return sourceDelegate.readLong();
    }

    @Override
    public long readLongLe() throws IOException {
        return sourceDelegate.readLongLe();
    }

    @Override
    public long readDecimalLong() throws IOException {
        return sourceDelegate.readDecimalLong();
    }

    @Override
    public long readHexadecimalUnsignedLong() throws IOException {
        return sourceDelegate.readHexadecimalUnsignedLong();
    }

    @Override
    public void skip(long byteCount) throws IOException {
        sourceDelegate.skip(byteCount);
    }

    @Override
    public ByteString readByteString() throws IOException {
        return sourceDelegate.readByteString();
    }

    @Override
    public ByteString readByteString(long byteCount) throws IOException {
        return sourceDelegate.readByteString(byteCount);
    }

    @Override
    public int select(Options options) throws IOException {
        return sourceDelegate.select(options);
    }

    @Override
    public byte[] readByteArray() throws IOException {
        return sourceDelegate.readByteArray();
    }

    @Override
    public byte[] readByteArray(long byteCount) throws IOException {
        return sourceDelegate.readByteArray(byteCount);
    }

    @Override
    public int read(byte[] sink) throws IOException {
        return sourceDelegate.read(sink);
    }

    @Override
    public void readFully(byte[] sink) throws IOException {
        sourceDelegate.readFully(sink);
    }

    @Override
    public int read(byte[] sink, int offset, int byteCount) throws IOException {
        return sourceDelegate.read(sink, offset, byteCount);
    }

    @Override
    public void readFully(Buffer sink, long byteCount) throws IOException {
        sourceDelegate.readFully(sink, byteCount);
    }

    @Override
    public long readAll(Sink sink) throws IOException {
        return sourceDelegate.readAll(sink);
    }

    @Override
    public String readUtf8() throws IOException {
        return sourceDelegate.readUtf8();
    }

    @Override
    public String readUtf8(long byteCount) throws IOException {
        return sourceDelegate.readUtf8(byteCount);
    }

    @Override
    public String readUtf8Line() throws IOException {
        return sourceDelegate.readUtf8Line();
    }

    @Override
    public String readUtf8LineStrict() throws IOException {
        return sourceDelegate.readUtf8LineStrict();
    }

    @Override
    public String readUtf8LineStrict(long limit) throws IOException {
        return sourceDelegate.readUtf8LineStrict(limit);
    }

    @Override
    public int readUtf8CodePoint() throws IOException {
        return sourceDelegate.readUtf8CodePoint();
    }

    @Override
    public String readString(Charset charset) throws IOException {
        return sourceDelegate.readString(charset);
    }

    @Override
    public String readString(long byteCount, Charset charset) throws IOException {
        return sourceDelegate.readString(byteCount, charset);
    }

    @Override
    public long indexOf(byte b) throws IOException {
        return sourceDelegate.indexOf(b);
    }

    @Override
    public long indexOf(byte b, long fromIndex) throws IOException {
        return sourceDelegate.indexOf(b, fromIndex);
    }

    @Override
    public long indexOf(byte b, long fromIndex, long toIndex) throws IOException {
        return sourceDelegate.indexOf(b, fromIndex, toIndex);
    }

    @Override
    public long indexOf(ByteString bytes) throws IOException {
        return sourceDelegate.indexOf(bytes);
    }

    @Override
    public long indexOf(ByteString bytes, long fromIndex) throws IOException {
        return sourceDelegate.indexOf(bytes, fromIndex);
    }

    @Override
    public long indexOfElement(ByteString targetBytes) throws IOException {
        return sourceDelegate.indexOfElement(targetBytes);
    }

    @Override
    public long indexOfElement(ByteString targetBytes, long fromIndex) throws IOException {
        return sourceDelegate.indexOfElement(targetBytes, fromIndex);
    }

    @Override
    public boolean rangeEquals(long offset, ByteString bytes) throws IOException {
        return sourceDelegate.rangeEquals(offset, bytes);
    }

    @Override
    public boolean rangeEquals(long offset, ByteString bytes, int bytesOffset, int byteCount) throws IOException {
        return sourceDelegate.rangeEquals(offset, bytes, bytesOffset, byteCount);
    }

    @Override
    public InputStream inputStream() {
        return sourceDelegate.inputStream();
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        return sourceDelegate.read(sink, byteCount);
    }

    @Override
    public Timeout timeout() {
        return sourceDelegate.timeout();
    }

    @Override
    public void close() throws IOException {
        sourceDelegate.close();
    }
}
