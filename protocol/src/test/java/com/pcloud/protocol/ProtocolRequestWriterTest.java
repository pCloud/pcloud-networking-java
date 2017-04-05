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
import com.pcloud.protocol.streaming.TypeToken;
import okio.BufferedSink;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
                .writeMethodName(null)
                .writeName(null, TypeToken.BOOLEAN);
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
    public void beginDataRequest_Throws_If_Request_Already_Started() throws Exception {
        writer.beginRequest();
        exception.expect(IllegalStateException.class);
        writer.beginRequest();
    }

    @Test
    public void name() throws Exception {
        writer.beginRequest()
                .writeMethodName("command")
                .writeName("key", TypeToken.NUMBER).writeValue(1)
                .writeName("value", TypeToken.BOOLEAN).writeValue(true)
                .writeName("param", TypeToken.STRING).writeValue("something")
                .writeData(null)
                .endRequest()
                .flush();
    }
}