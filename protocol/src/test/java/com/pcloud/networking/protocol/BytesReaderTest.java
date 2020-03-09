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

import com.pcloud.utils.DummyBufferedSource;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class BytesReaderTest {

    private BytesReader reader;
    private BufferedSource source;
    private Buffer buffer;

    private static final ByteString MOCK_RESPONSE = new ResponseBytesWriter()
            .beginObject()
            .writeValue("result", 0L)
            .endObject()
            .bytes();
    private static final ByteString MOCK_DATA = ByteString.encodeUtf8("some data");
    private static final ByteString MOCK_DATA_RESPONSE = new ResponseBytesWriter()
            .beginObject()
            .writeValue("result", 0L)
            .writeValue("somefield", "someData")
            .setData(MOCK_DATA)
            .endObject()
            .bytes();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        buffer = new Buffer();
        source = Mockito.spy(new DummyBufferedSource(buffer));
        reader = new BytesReader(source);
        setIncomingResponse(MOCK_RESPONSE);
    }

    @Test
    public void constructor_Throws_On_Null_Argument() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        reader = new BytesReader(null);
    }

    @Test
    public void close_Calls_Close_On_Source() throws Exception {

        reader.close();
        Mockito.verify(source).close();
    }

    @Test
    public void beginResponse_Throws_If_Called_Twice_In_A_Row() throws Exception {
        expectedException.expect(IllegalStateException.class);
        reader.beginResponse();
        reader.beginResponse();
    }

    @Test
    public void endResponse_Throws_If_Called_Before_beginResponse() throws Exception {
        expectedException.expect(IllegalStateException.class);
        reader.endResponse();
    }

    @Test
    public void beginObject_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.beginObject();
    }

    @Test
    public void beginArray_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.beginArray();
    }

    @Test
    public void readNumber_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.readNumber();
    }

    @Test
    public void readBoolean_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.readBoolean();
    }

    @Test
    public void readString_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.readString();
    }

    @Test
    public void endObject_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.endObject();
    }

    @Test
    public void endArray_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.endArray();
    }

    @Test
    public void skipValue_Throws_If_Called_Before_beginResponse() throws Exception {

        expectedException.expect(IllegalStateException.class);
        reader.skipValue();
    }

    @Test
    public void dataContentLength_Should_Be_Equal_To_Actual_Length_Of_Data() throws Exception {
        setIncomingResponse(MOCK_DATA_RESPONSE);
        reader.beginResponse();

        while (reader.hasNext()) {
            reader.skipValue();
        }

        reader.endResponse();
        assertTrue(reader.dataContentLength() == MOCK_DATA.size());
    }

    @Test
    public void readString_ShouldNotFail() throws Exception {

        reader.beginResponse();
        reader.beginObject();
        assertNotNull(reader.readString());
    }

    @Test
    public void readNumber_ShouldNotFail() throws Exception {
        reader.beginResponse();
        reader.beginObject();
        reader.readString();
        reader.readNumber();
    }

    @Test
    public void readBoolean_Throws_On_Wrong_Type() throws Exception {

        expectedException.expect(SerializationException.class);
        reader.beginResponse();
        reader.beginObject();
        //the next element is a String so this should throw
        reader.readBoolean();
    }

    private void setIncomingResponse(ByteString responseBytes) {
        buffer.clear();
        buffer.write(responseBytes);
    }

    @Test
    public void buffer_Should_Be_Exhausted_After_Reading_Whole_Response() throws Exception {

        reader.beginResponse();

        while (reader.hasNext()) {
            reader.skipValue();
        }

        reader.endResponse();

        assertTrue(source.exhausted());
    }

    @Test
    public void scope_Should_Be_None_Before_beginResponse() throws Exception {

        //the reader should have SCOPE_NONE at the beginning
        assertEquals(reader.currentScope(), BytesReader.SCOPE_NONE);
    }

    @Test
    public void scope_Should_Be_SCOPE_RESPONSE_After_beginResponse() throws Exception {

        reader.beginResponse();
        //the scope should be SCOPE_RESPONSE when we start reading
        assertEquals(reader.currentScope(), BytesReader.SCOPE_RESPONSE);
    }

    @Test
    public void scope_Should_Be_SCOPE_OBJECT_After_beginObject() throws Exception {

        reader.beginResponse();
        reader.beginObject();
        //the scope should be SCOPE_OBJECT when we begin to read an object
        assertEquals(reader.currentScope(), BytesReader.SCOPE_OBJECT);
    }

    @Test
    public void scope_Should_Be_SCOPE_RESPONSE_After_Reading_The_Whole_Response() throws Exception {

        reader.beginResponse();

        //reading the whole response
        while (reader.hasNext()) {
            reader.skipValue();
        }

        //when we have read the whole response but haven't yet called endResponse() the scope should be SCOPE_RESPONSE
        assertEquals(reader.currentScope(), BytesReader.SCOPE_RESPONSE);
    }

    @Test
    public void scope_Should_Be_SCOPE_NONE_After_endResponse() throws Exception {

        reader.beginResponse();

        //reading the whole response
        while (reader.hasNext()) {
            reader.skipValue();
        }

        reader.endResponse();
        //we have read the whole response and called endResponse() so we should be at SCOPE_NONE
        assertEquals(reader.currentScope(), BytesReader.SCOPE_NONE);
    }

    @Test
    public void scope_Should_Be_SCOPE_DATA_After_endResponse_When_Reading_Data_Response() throws Exception {
        setIncomingResponse(MOCK_DATA_RESPONSE);
        reader.beginResponse();

        //reading the whole response
        while (reader.hasNext()) {
            reader.skipValue();
        }

        reader.endResponse();
        //we have read the whole response and called endResponse() so we should be at SCOPE_NONE
        assertEquals(reader.currentScope(), ProtocolResponseReader.SCOPE_DATA);
    }

    @Test
    public void peekingReader_Should_Not_Return_Null() throws Exception {

        assertNotNull(reader.newPeekingReader());
    }

    @Test
    public void peekingReader_Should_Have_The_Same_Scope() throws Exception {

        reader.beginResponse();
        int readerScope = reader.currentScope();
        ProtocolResponseReader newReader = reader.newPeekingReader();
        int newReaderScope = newReader.currentScope();

        assertEquals(readerScope, newReaderScope);
    }

    @Test
    public void peekingReader_Should_Have_The_Same_Buffer_Contents() throws Exception {

        Buffer newBuffer = buffer.clone();
        ProtocolResponseReader newReader = reader.newPeekingReader();
        newReader.beginResponse();

        while (newReader.hasNext()) {
            newReader.skipValue();
        }
        newReader.endResponse();

        assertEquals(newBuffer, buffer);
    }

    @Test
    public void peekingReader_Should_Not_Consume_Any_Data() throws Exception {

        ProtocolResponseReader peekingReader = reader.newPeekingReader();


        assertEquals(peekingReader.beginResponse(), reader.beginResponse());
        peekingReader.beginObject();
        reader.beginObject();

        while (reader.hasNext()) {
            TypeToken token = peekingReader.peek();
            assertEquals(peekingReader.peek(), reader.peek());
            switch (token) {
                case NUMBER:
                    assertEquals(peekingReader.readNumber(), reader.readNumber());
                    break;

                case STRING:
                    assertEquals(peekingReader.readString(), reader.readString());
                    break;

                case BOOLEAN:
                    assertEquals(peekingReader.readBoolean(), reader.readBoolean());
                    break;
            }
        }
    }

    @Test
    public void peekingReader_readData_Should_Fail_1() throws Exception {
        setIncomingResponse(MOCK_DATA_RESPONSE);
        expectedException.expect(UnsupportedOperationException.class);
        reader.newPeekingReader().readData(Okio.buffer(Okio.blackhole()));
    }

    @Test
    public void peekingReader_readData_Should_Fail_2() throws Exception {
        setIncomingResponse(MOCK_DATA_RESPONSE);
        expectedException.expect(UnsupportedOperationException.class);
        reader.newPeekingReader().readData(Okio.buffer(Okio.blackhole()).outputStream());
    }

    @Test
    public void peek_Should_Not_Change_Buffer_Size() throws Exception {
        reader.beginResponse();
        long sizeBefore = buffer.size();
        reader.peek();
        long sizeAfter = buffer.size();
        assertEquals(sizeBefore, sizeAfter);
    }

    @Test
    public void peekingReader_Should_Not_Close_The_Real_Buffer() throws Exception {
        reader.newPeekingReader().close();
        Mockito.verify(source, Mockito.never()).close();
    }

    @Test
    public void peek_throws_IllegalStateException_If_Called_Outside_Response_Scope() throws Exception {
        expectedException.expect(IllegalStateException.class);
        reader.peek();
    }

    @Test
    public void peek_Returns_Begin_Object_After_beginResponse_Called() throws Exception {
        reader.beginResponse();
        assertEquals(TypeToken.BEGIN_OBJECT, reader.peek());
    }

    @Test
    public void peek_Returns_Begin_Object_After_beginObject_Called() throws Exception {

        reader.beginResponse();
        assertEquals(TypeToken.BEGIN_OBJECT, reader.peek());
    }

    @Test
    public void peek_Returns_Begin_Object_Before_Reading_Nested_Object() throws Exception {
        setIncomingResponse(new ResponseBytesWriter()
                .beginObject()
                .writeKey("object").beginObject()
                .endObject()
                .endObject()
                .bytes()
        );
        reader.beginResponse();
        reader.beginObject();
        while (reader.hasNext()) {
            if ("object".equals(reader.readString())) {
                assertEquals(TypeToken.BEGIN_OBJECT, reader.peek());
                return;
            } else {
                reader.skipValue();
            }
        }
    }

    @Test
    public void peek_Returns_Begin_Array_Before_Reading_Array_Value() throws Exception {
        setIncomingResponse(new ResponseBytesWriter()
                .beginObject()
                .writeKey("object").beginArray().endArray()
                .endObject()
                .bytes()
        );
        reader.beginResponse();
        reader.beginObject();
        while (reader.hasNext()) {
            if ("object".equals(reader.readString())) {
                assertEquals(TypeToken.BEGIN_ARRAY, reader.peek());
                return;
            } else {
                reader.skipValue();
            }
        }
    }
}
