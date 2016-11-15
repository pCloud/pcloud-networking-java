package com.pcloud.internal;

import java.io.Closeable;
import java.net.Socket;

/**
 * Created by Georgi Neykov on 8.11.2016.
 */

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
