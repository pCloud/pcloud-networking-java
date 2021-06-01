
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

package com.pcloud.networking.protocol;

import com.pcloud.utils.DummyBufferedSink;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class BytesWriterTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private ProtocolRequestWriter writer;
    private BufferedSink dataSink;

    @Before
    public void setUp() {
        BufferedSink buffer = new Buffer();
        dataSink = spy(new DummyBufferedSink(buffer));
        writer = new BytesWriter(dataSink);
    }

    @Test
    public void constructor_throws_Illegal_Argument_On_Null_Sink_Argument() throws Exception {
        exception.expect(IllegalArgumentException.class);
        new BytesWriter(null);
    }

    @Test
    public void method_Throws_On_Null_Argument() throws IOException {
        exception.expect(IllegalArgumentException.class);
        writer.beginRequest()
                .writeMethodName(null);
    }

    @Test
    public void data_Throws_On_Null_Source_Argument() throws IOException {
        exception.expect(IllegalArgumentException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeData(null);
    }

    @Test
    public void parameter_Throws_On_Null_Name_Argument() throws IOException {
        exception.expect(IllegalArgumentException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName(null);
    }

    @Test
    public void flush_Calls_Flush_On_Wrapped_Sink() throws Exception {
        writer.flush();
        verify(dataSink).flush();
    }

    @Test
    public void close_Closes_Wrapped_Sink() throws Exception {
        writer.close();
        verify(dataSink).close();
    }

    @Test
    public void beginRequest_Throws_If_Request_Already_Started() throws Exception {
        writer.beginRequest();
        exception.expect(IllegalStateException.class);
        writer.beginRequest();
    }

    @Test
    public void endRequest_Throws_If_WriteName_Was_Not_Called() throws Exception {
        exception.expect(IllegalArgumentException.class);
        writer.beginRequest();
        writer.endRequest();
    }

    @Test
    public void writeMethodName_Throws_If_Called_Twice() throws Exception {
        exception.expect(IllegalStateException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeMethodName("somemethod");
    }

    @Test
    public void endRequest_Throws_If_MethodName_Longer_Than_127_Bytes() throws Exception {
        exception.expect(SerializationException.class);
        writer.beginRequest()
                .writeMethodName("Lorem ipsum dolor sit amet, " +
                        "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum")
                .endRequest();
    }

    @Test
    public void endRequest_Throws_If_DataSource_Length_Is_0() throws Exception {
        exception.expect(SerializationException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeData(new DataSource() {
                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        //Empty
                    }
                }).endRequest();
    }

    @Test
    public void endRequest_Throws_On_Request_Longer_Than_65k_Bytes() throws Exception {
        exception.expect(SerializationException.class);
        writer.beginRequest()
                .writeMethodName("somemethod");
        for (int i = 0; i < 65000; i++) {
            writer.writeName("somename");
            writer.writeValue("somevalue");
        }
        writer.endRequest();
    }

    @Test
    public void writeName_Throws_If_Called_Twice_Without_writeValue() throws Exception {
        exception.expect(IllegalStateException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("somename")
                .writeName("somename")
                .endRequest();
    }

    @Test
    public void writeName_Throws_On_More_Than_225_Arguments() throws Exception {
        exception.expect(SerializationException.class);
        writer.beginRequest()
                .writeMethodName("somemethod");
        for (int i = 0; i < 256; i++) {

            writer.writeName("a");
            writer.writeValue(1);
        }
        writer.endRequest();
    }

    @Test
    public void writeValue_Throws_If_Param_Name_Exceeds_63() throws Exception {
        exception.expect(SerializationException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("Lorem ipsum dolor sit amet, " +
                        "consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum")
                .writeValue(1)
                .endRequest();
    }

    @Test
    public void writeValue_Throws_On_Null_Value() throws Exception {
        exception.expect(IllegalArgumentException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("somename")
                .writeValue(null)
                .endRequest();
    }

    @Test
    public void writeValue_Throws_If_Called_Twice_In_A_Row_Without_Calling_WriteName_First() throws Exception {
        exception.expect(IllegalStateException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("somename")
                .writeValue(1)
                .writeValue(1)
                .endRequest();
    }

    @Test
    public void writeData_Throws_If_Called_Twice() throws Exception {
        DataSource source = DataSource.create(ByteString.encodeUtf8("abc"));
        exception.expect(IllegalStateException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeData(source)
                .writeData(source)
                .endRequest();
    }

    @Test
    public void writeValue_Throws_If_Passed_Argument_Cannot_Be_Serialized() throws Exception {
        exception.expect(IllegalArgumentException.class);
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("somename")
                .writeValue(new ArrayList<>())
                .endRequest();
    }

    @Test
    public void endRequest_Calls_Emit_On_Sink() throws Exception {
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("somename")
                .writeValue("somevalue")
                .endRequest();
        verify(dataSink, times(1)).emit();
    }

    private static final ByteString TEST_REQUEST_BYTES = ByteString.decodeHex(
            "69000a736f6d656d6574686f6406446c6f6e67010000000000000087626f6f6c65616e010673" +
                    "7472696e6709000000736f6d657468696e6705666c6f617403000000322e3306646f" +
                    "75626c6504000000322e3035106e6567617469766520696e7465676572020000002d31");
    private static final ByteString TEST_REQUEST_WITH_DATA_BYTES = ByteString.decodeHex(
            "71008a0300000000000000736f6d656d6574686f6406446c6f6e67010000000000000087626f" +
                    "6f6c65616e0106737472696e6709000000736f6d657468696e6705666c6f61740300" +
                    "0000322e3306646f75626c6504000000322e3035106e6567617469766520696e7465" +
                    "676572020000002d31616263");

    @Test
    public void protocolWriter_writes_Complete_Request_Correctly() throws Exception {
        writeRequestWithoutData(writer);
        ByteString sentBytes = dataSink.buffer().readByteString();
        assertEquals(TEST_REQUEST_BYTES, sentBytes);
    }

    @Test
    public void protocolWriter_writes_Complete_Request_With_Data_Correctly() throws Exception {

        writeRequestWithData(writer);
        ByteString sentBytes = dataSink.buffer().readByteString();
        assertEquals(TEST_REQUEST_WITH_DATA_BYTES, sentBytes);
    }

    @Test
    public void protocolWriter_Writes_Multiple_Requests_Correctly() throws Exception {
        writeRequestWithoutData(writer);
        writeRequestWithData(writer);
        writeRequestWithData(writer);
        writeRequestWithoutData(writer);
        writeRequestWithData(writer);

        dataSink.flush();
        ByteString sentBytes = dataSink.buffer().readByteString();
        ByteString expectedBytes = new Buffer()
                .write(TEST_REQUEST_BYTES)
                .write(TEST_REQUEST_WITH_DATA_BYTES)
                .write(TEST_REQUEST_WITH_DATA_BYTES)
                .write(TEST_REQUEST_BYTES)
                .write(TEST_REQUEST_WITH_DATA_BYTES)
                .readByteString();
        assertTrue(String.format("Expected and sent bytes are different." +
                        "\nExpected: (%d bytes) [%s]" +
                        "\n  Actual: (%d bytes) [%s]", expectedBytes.size(), expectedBytes.hex(),
                sentBytes.size(), sentBytes.hex()),
                sentBytes.equals(expectedBytes));
    }

    private static void writeRequestWithoutData(ProtocolRequestWriter writer) throws IOException {
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("long").writeValue(1)
                .writeName("boolean").writeValue(true)
                .writeName("string").writeValue("something")
                .writeName("float").writeValue(2.3f)
                .writeName("double").writeValue(2.05d)
                .writeName("negative integer").writeValue(-1L)
                .endRequest();
    }

    private static void writeRequestWithData(ProtocolRequestWriter writer) throws IOException {
        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("long").writeValue(1)
                .writeName("boolean").writeValue(true)
                .writeName("string").writeValue("something")
                .writeName("float").writeValue(2.3f)
                .writeName("double").writeValue(2.05d)
                .writeName("negative integer").writeValue(-1L)
                .writeData(DataSource.create(ByteString.encodeUtf8("abc")))
                .endRequest();
    }
}