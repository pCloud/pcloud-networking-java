/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Georgi Neykov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.pcloud;

import okio.Buffer;
import okio.BufferedSource;
import okio.Source;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class IOUtils {

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (AssertionError e) {
                if (!isAndroidGetsocknameError(e)) throw e;
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Returns true if {@code e} is due to a firmware bug fixed after Android 4.2.2.
     * https://code.google.com/p/android/issues/detail?id=54072
     */
    public static boolean isAndroidGetsocknameError(AssertionError e) {
        return e.getCause() != null && e.getMessage() != null
                && e.getMessage().contains("getsockname failed");
    }

    /**
     * Reads until {@code in} is exhausted or the deadline has been reached. This is careful to not
     * extend the deadline if one exists already.
     */
    public static boolean skipAll(Source source, int duration, TimeUnit timeUnit) throws IOException {
        long now = System.nanoTime();
        long originalDuration = source.timeout().hasDeadline()
                ? source.timeout().deadlineNanoTime() - now
                : Long.MAX_VALUE;
        source.timeout().deadlineNanoTime(now + Math.min(originalDuration, timeUnit.toNanos(duration)));
        try {
            Buffer skipBuffer = new Buffer();
            while (source.read(skipBuffer, 8192) != -1) {
                skipBuffer.clear();
            }
            return true; // Success! The source has been exhausted.
        } catch (InterruptedIOException e) {
            return false; // We ran out of time before exhausting the source.
        } finally {
            if (originalDuration == Long.MAX_VALUE) {
                source.timeout().clearDeadline();
            } else {
                source.timeout().deadlineNanoTime(now + originalDuration);
            }
        }
    }

    public static long readNumberLe(BufferedSource source, int byteCount) throws IOException {
        source.require(byteCount);
        if (byteCount > 1) {
            byte[] number = source.readByteArray(byteCount);
            long value = 0;
            long m = 1;
            for (int i = 0; i < byteCount; i++) {
                value += m * (number[i] & 0xff);
                m *= 256;
            }
            return value;
        } else {
            return source.readByte() & 0xff;
        }
    }

    public static long peekNumberLe(BufferedSource source, int byteCount) throws IOException {
        source.require(byteCount);
        if (byteCount > 1) {
            Buffer numberBytes = new Buffer();
            source.buffer().copyTo(numberBytes, 0, byteCount);
            long value = 0;
            long m = 1;
            for (int i = 0; i < byteCount; i++) {
                value += m * (numberBytes.getByte(i) & 0xff);
                m *= 256;
            }
            return value;
        } else {
            return source.buffer().getByte(0) & 0xff;
        }
    }
}
