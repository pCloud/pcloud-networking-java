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

package com.pcloud.protocol;

import com.pcloud.protocol.streaming.BytesWriter;
import com.pcloud.protocol.streaming.ProtocolRequestWriter;
import com.pcloud.protocol.streaming.SerializationException;
import com.pcloud.protocol.streaming.TypeToken;
import okio.Buffer;
import okio.BufferedSink;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ProtocolRequestWriterTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ProtocolRequestWriter writer;
    private BufferedSink dataSink;

    @Before
    public void setUp() {
        dataSink = spy(BufferedSink.class);
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
    public void close_Closes_Wrapped_Sink() throws Exception {
        writer.close();
        verify(dataSink).close();
    }

    @Test
    public void flush_Flushes_Wrapped_Sink() throws Exception {
        writer.flush();
        verify(dataSink).flush();
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
    public void endRequest_Throws_If_MethodName_Longer_Than_128() throws Exception {

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

        DataSource source = new DataSource() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.writeByte(123);
                sink.writeByte(123);
            }
        };
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
    public void endRequest_Calls_Write_On_Sink_Twice() throws Exception {

        writer.beginRequest()
                .writeMethodName("somemethod")
                .writeName("somename")
                .writeValue("somevalue")
                .endRequest();
        verify(dataSink, times(2)).write(any(Buffer.class),any(Long.class));
        verify(dataSink).writeShortLe(any(Integer.class));
    }


    @Test
    public void name() throws Exception {
        writer.beginRequest()
                .writeMethodName("command")
                .writeName("key").writeValue(1)
                .writeName("value").writeValue(true)
                .writeName("param").writeValue("something")
                .writeData(null)
                .endRequest()
                .flush();
    }
}