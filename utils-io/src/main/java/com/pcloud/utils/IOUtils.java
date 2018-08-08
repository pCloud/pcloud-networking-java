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
import okio.BufferedSource;
import okio.Source;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to help with IO operations
 */
public class IOUtils {

    private static final int HEX_255 = 0xff;
    private static final int EIGHT_KB = 8192;
    public static final int BITS_PER_BYTE = 8;
    public static final long HEX_255_LONG = 0xffL;

    /**
     * Closes all {@linkplain Closeable} objects from an array
     *
     * @param closeables an array of {@linkplain Closeable} to be closed
     */
    public static void closeQuietly(Closeable[] closeables) {
        for (Closeable c : closeables) {
            closeQuietly(c);
        }
    }

    /**
     * Closes all {@linkplain Closeable} objects from a {@linkplain Iterable}
     *
     * @param closeables an {@linkplain Iterable} of {@linkplain Closeable} to be closed
     */
    public static void closeQuietly(Iterable<? extends Closeable> closeables) {
        for (Closeable c : closeables) {
            closeQuietly(c);
        }
    }

    /**
     * Closes a {@linkplain Closeable} object
     *
     * @param closeable the {@linkplain Closeable} object to be closed
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
                //Empty
            }
        }
    }

    /**
     * Closes a {@linkplain Socket} object
     *
     * @param socket the {@linkplain Socket} to be closed
     */
    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (AssertionError e) {
                if (!isAndroidGetsocknameError(e)) throw e;
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
                //Empty
            }
        }
    }

    /**
     * Returns true if {@code e} is due to a firmware bug fixed after Android 4.2.2.
     * https://code.google.com/p/android/issues/detail?id=54072
     *
     * @param e the suspected error
     * @return true if error is due to firmware bug, false otherwise
     */
    public static boolean isAndroidGetsocknameError(AssertionError e) {
        return e.getCause() != null && e.getMessage() != null &&
                e.getMessage().contains("getsockname failed");
    }

    /**
     * Goes through the {@linkplain Source} and consumes all the contents
     *
     * @param source the {@linkplain Source} to consume data from
     * @throws IOException upon failed IO operations
     */
    public static void skipAll(Source source) throws IOException {
        Buffer skipBuffer = new Buffer();
        while (source.read(skipBuffer, EIGHT_KB) != -1) {
            skipBuffer.clear();
        }
    }

    /**
     * Reads until {@code in} is exhausted or the deadline has been reached. This is careful to not
     * extend the deadline if one exists already.
     *
     * @param source   the target source to be exhausted
     * @param duration the time available for exhaustion
     * @param timeUnit the time unit of exhaustion time
     * @return true if the source was exhausted without closing, false if time ran out
     * @throws IOException on an IO error
     */
    public static boolean skipAll(Source source, int duration, TimeUnit timeUnit) throws IOException {
        long now = System.nanoTime();
        long originalDuration = source.timeout().hasDeadline() ?
                source.timeout().deadlineNanoTime() - now :
                Long.MAX_VALUE;
        source.timeout().deadlineNanoTime(now + Math.min(originalDuration, timeUnit.toNanos(duration)));
        try {
            Buffer skipBuffer = new Buffer();
            while (source.read(skipBuffer, EIGHT_KB) != -1) {
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

    /**
     * Reads a number from the {@linkplain BufferedSource}
     *
     * @param source    the data source to read from
     * @param byteCount the number of bytes to read
     * @return the number extracted from the source
     * @throws IOException on failed IO operations
     */
    public static long readNumberLe(BufferedSource source, int byteCount) throws IOException {
        source.require(byteCount);
        if (byteCount > 1) {
            long value = 0;
            for (int i = 0, shift = 0; i < byteCount; i++, shift += BITS_PER_BYTE) {
                value += ((long) source.readByte() & HEX_255_LONG) << shift;
            }
            return value;
        } else {
            return source.readByte() & HEX_255;
        }
    }

    /**
     * Peeks but does not consume a number from a {@linkplain BufferedSource}
     *
     * @param source    the data source to peek from
     * @param offset    marks where should the peeking begin
     * @param byteCount the number of bytes to read
     * @return the number extracted form the {@linkplain BufferedSource}
     * @throws IOException on failed IO operations
     */
    public static long peekNumberLe(BufferedSource source, int offset, int byteCount) throws IOException {
        source.require(offset + byteCount);
        Buffer buffer = source.buffer();
        if (byteCount > 1) {
            long value = 0;
            for (int i = 0, shift = 0; i < byteCount; i++, shift += BITS_PER_BYTE) {
                value += ((long) buffer.getByte(offset + i) & HEX_255_LONG) << shift;
            }
            return value;
        } else {
            return buffer.getByte(offset) & HEX_255;
        }
    }

    /**
     * Peeks but does not consume a number from a {@linkplain BufferedSource}
     *
     * @param source    the data source to peek from
     * @param byteCount the number of bytes to peek
     * @return the number value extracted from the source
     * @throws IOException on an IO error
     */
    public static long peekNumberLe(BufferedSource source, int byteCount) throws IOException {
        return peekNumberLe(source, 0, byteCount);
    }
}
