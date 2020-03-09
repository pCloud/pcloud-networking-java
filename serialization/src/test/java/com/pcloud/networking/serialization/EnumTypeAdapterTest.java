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

package com.pcloud.networking.serialization;

import com.pcloud.networking.protocol.*;
import okio.Buffer;
import okio.ByteString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class EnumTypeAdapterTest {

    private enum StringEnumType {
        @ParameterValue("something1")
        FIRST_VALUE,

        @ParameterValue("something2")
        SECOND_VALUE,

        @ParameterValue("something3")
        THIRD_VALUE,
    }

    private enum NumberEnumType {
        @ParameterValue("5000")
        FIRST_VALUE,

        @ParameterValue("2")
        SECOND_VALUE,

        @ParameterValue("10")
        THIRD_VALUE,
    }

    private enum DuplicateNamesEnumType1 {
        @ParameterValue("something")
        FIRST_VALUE,

        @ParameterValue("something")
        SECOND_VALUE,

        @ParameterValue("something")
        THIRD_VALUE,
    }

    private enum DuplicateNamesEnumType2 {
        @ParameterValue("something")
        something1,

        something,
    }

    private enum DuplicateNamesEnumType3 {
        @ParameterValue("1")
        FIRST_VALUE,

        @ParameterValue("1")
        SECOND_VALUE,
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void returns_Correct_Constant_When_Deserializing_Strings() throws Exception {
        ProtocolReader reader = readerWithValues("something1", "something2", "something3");

        TypeAdapter<StringEnumType> adapter = new EnumTypeAdapter<>(StringEnumType.class);

        assertEquals(StringEnumType.FIRST_VALUE, adapter.deserialize(reader));
        assertEquals(StringEnumType.SECOND_VALUE, adapter.deserialize(reader));
        assertEquals(StringEnumType.THIRD_VALUE, adapter.deserialize(reader));
    }

    @Test
    public void returns_Correct_Constant_When_Deserializing_Numbers() throws Exception {
        ProtocolReader reader = readerWithValues(5000, 2, 10);

        TypeAdapter<NumberEnumType> adapter = new EnumTypeAdapter<>(NumberEnumType.class);

        assertEquals(NumberEnumType.FIRST_VALUE, adapter.deserialize(reader));
        assertEquals(NumberEnumType.SECOND_VALUE, adapter.deserialize(reader));
        assertEquals(NumberEnumType.THIRD_VALUE, adapter.deserialize(reader));
    }

    @Test
    public void throws_On_Duplicate_Type_Annotations_When_Initialized1() throws Exception {
        expectedException.expect(IllegalStateException.class);
        TypeAdapter<DuplicateNamesEnumType1> adapter = new EnumTypeAdapter<>(DuplicateNamesEnumType1.class);
    }

    @Test
    public void throws_On_Duplicate_Type_Annotations_When_Initialized2() throws Exception {
        expectedException.expect(IllegalStateException.class);
        TypeAdapter<DuplicateNamesEnumType2> adapter = new EnumTypeAdapter<>(DuplicateNamesEnumType2.class);
    }

    @Test
    public void throws_On_Duplicate_Type_Annotations_When_Initialized3() throws Exception {
        expectedException.expect(IllegalStateException.class);
        TypeAdapter<DuplicateNamesEnumType2> adapter = new EnumTypeAdapter<>(DuplicateNamesEnumType2.class);
    }

    @Test
    public void throws_On_Invalid_Protocol_Type1() throws Exception {
        ProtocolReader reader = readerWithValues(false, true, false);
        TypeAdapter<NumberEnumType> adapter = new EnumTypeAdapter<>(NumberEnumType.class);
        expectedException.expect(SerializationException.class);
        adapter.deserialize(reader);
    }

    @Test
    public void throws_On_Invalid_Protocol_Type2() throws Exception {
        ProtocolReader reader = readerWithValues();
        TypeAdapter<NumberEnumType> adapter = new EnumTypeAdapter<>(NumberEnumType.class);
        expectedException.expect(SerializationException.class);
        adapter.deserialize(reader);
    }

    private static ProtocolReader readerWithValues(Object... values) throws IOException {
        ByteString response = ResponseBytesWriter.empty()
                .beginObject()
                .writeValue("values", values)
                .endObject()
                .bytes();
        ProtocolResponseReader reader = new BytesReader(new Buffer().write(response));
        reader.beginResponse();
        reader.beginObject();
        reader.readString();
        reader.beginArray();
        return reader;
    }
}