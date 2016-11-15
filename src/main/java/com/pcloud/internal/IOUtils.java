/*
 * Copyright (c) 2016 Georgi Neykov
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

package com.pcloud.internal;

import java.io.Closeable;
import java.net.Socket;

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

    public static long reverseBytesLong(long v) {
        return (v & 0xff00000000000000L) >>> 56
                |  (v & 0x00ff000000000000L) >>> 40
                |  (v & 0x0000ff0000000000L) >>> 24
                |  (v & 0x000000ff00000000L) >>>  8
                |  (v & 0x00000000ff000000L)  <<  8
                |  (v & 0x0000000000ff0000L)  << 24
                |  (v & 0x000000000000ff00L)  << 40
                |  (v & 0x00000000000000ffL)  << 56;
    }
}
