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

package com.pcloud.utils;


import okio.Buffer;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;

public class IOUtilsTest {

    @Test
    public void readNumber_Reads_Numbers_Correctly() throws Exception {
        for (int byteCount = 1; byteCount <= 8; byteCount++) {
            assertNumberReadCorrectly(byteCount);
        }
    }

    @Test
    public void peekNumber_Peeks_Numbers_Correctly() throws Exception {
        for (int byteCount = 1; byteCount <= 8; byteCount++) {
            assertNumberPeekedCorrectly(byteCount);
        }
    }

    @Test
    public void peekNumber_Peeks_Numbers_With_Offset_Correctly() throws Exception {
        for (int byteCount = 1; byteCount <= 8; byteCount++) {
            for (int offset = 1; offset <= 63; offset++) {
                assertNumberPeekedCorrectly(byteCount, offset);
            }
        }
    }

    private static void assertNumberReadCorrectly(int byteCount) throws IOException {
        long expected = generateNumber(byteCount);
        byte[] bytes = numberToLittleEndianByteArray(expected, byteCount);


        Buffer okioSource = new Buffer();
        okioSource.write(bytes);
        long actual = IOUtils.readNumberLe(okioSource, byteCount);
        assertEquals(expected, actual);
        assertEquals(0, okioSource.size());
    }

    private static void assertNumberPeekedCorrectly(int byteCount) throws IOException {
        long expected = generateNumber(byteCount);
        byte[] bytes = numberToLittleEndianByteArray(expected, byteCount);

        Buffer okioSource = new Buffer();
        okioSource.write(bytes);
        long actual = IOUtils.peekNumberLe(okioSource, byteCount);
        assertEquals(expected, actual);
        assertEquals(bytes.length, okioSource.size());
        for (int i = 0; i < byteCount; i++) {
            assertEquals(bytes[i], okioSource.readByte());
        }
    }

    private static void assertNumberPeekedCorrectly(int byteCount, int offset) throws IOException {
        long expected = generateNumber(byteCount);
        byte[] bytes = numberToLittleEndianByteArray(expected, byteCount);


        Buffer okioSource = new Buffer();
        // Generate some garbage bytes.
        for (int i = 0 ; i < offset; i++) {
            okioSource.writeByte((byte) (System.nanoTime() >>>  8));
        }
        okioSource.write(bytes);
        long actual = IOUtils.peekNumberLe(okioSource, offset, byteCount);
        assertEquals(expected, actual);
        assertEquals(offset + byteCount, okioSource.size());
    }

    private static long generateNumber(int byteCount) {
        long result = 0;
        for (int i = 0, octet = 0xff; i < byteCount; i++, octet -= 0x01) {
            result += octet;
            result = result << i;
        }
        return result;
    }

    private static byte[] numberToLittleEndianByteArray(long value, int byteCount) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(value);
        buffer.flip();
        byte[] result = new byte[byteCount];
        buffer.get(result, 0, byteCount);
        return result;
    }
}