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

    private static final int HEX_255 = 0xff;
    private static final int EIGHT_KB = 8192;
    private static final int NEXT_BYTE_POSITION = 256;

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
     */
    public static boolean isAndroidGetsocknameError(AssertionError e) {
        return e.getCause() != null && e.getMessage() != null &&
                       e.getMessage().contains("getsockname failed");
    }

    /**
     * Reads until {@code in} is exhausted or the deadline has been reached. This is careful to not
     * extend the deadline if one exists already.
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

    public static long readNumberLe(BufferedSource source, int byteCount) throws IOException {
        source.require(byteCount);
        if (byteCount > 1) {
            byte[] number = source.readByteArray(byteCount);
            long value = 0;
            long m = 1;
            for (int i = 0; i < byteCount; i++) {
                value += m * (number[i] & HEX_255);
                m *= NEXT_BYTE_POSITION;
            }
            return value;
        } else {
            return source.readByte() & HEX_255;
        }
    }

    public static long peekNumberLe(BufferedSource source, int offset, int byteCount) throws IOException {
        source.require(offset + byteCount);
        if (byteCount > 1) {
            Buffer numberBytes = new Buffer();
            source.buffer().copyTo(numberBytes, offset, byteCount);
            long value = 0;
            long m = 1;
            for (int i = 0; i < byteCount; i++) {
                value += m * (numberBytes.getByte(i) & HEX_255);
                m *= NEXT_BYTE_POSITION;
            }
            return value;
        } else {
            return source.buffer().getByte(0) & HEX_255;
        }
    }

    public static long peekNumberLe(BufferedSource source, int byteCount) throws IOException {
        return peekNumberLe(source, 0, byteCount);
    }
}
