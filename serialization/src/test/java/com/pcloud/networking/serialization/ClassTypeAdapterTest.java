package com.pcloud.networking.serialization;

import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolResponseReader;
import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.protocol.ResponseBytesWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ClassTypeAdapterTest {

    private static final String PARAMETER_NAME = "parameter";

    private ProtocolWriter writer;
    private Transformer transformer;
    private TypeAdapter<CustomType> typeAdapter;

    @Before
    public void setUp() {
        writer = mock(ProtocolWriter.class);
        typeAdapter = spy(new CustomTypeAdapter());
        transformer = Transformer.create().addTypeAdapter(CustomType.class, typeAdapter).build();
    }

    /*
     * Boolean
     * */

    @SuppressWarnings("ConstantConditions")
    @Test
    public void boolean_Fields_Serialize_Correctly_For_Primitive_Values() throws IOException {
        Booleans object = new Booleans();
        boolean expected = true;
        object.aBoolean = expected;
        transformer.getTypeAdapter(Booleans.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeValue(eq(expected));
        verifyNoMoreInteractions(writer);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void boolean_Fields_Serialize_Correctly_For_Boxed_Values() throws IOException {
        Booleans object = new Booleans();
        Boolean expected = Boolean.TRUE;
        object.aBoolean = expected;
        object.aBooleanObject = expected;
        transformer.getTypeAdapter(Booleans.class).serialize(writer, object);
        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName("aBooleanObject");
        verify(writer, times(2)).writeValue(eq(expected.booleanValue()));
        verifyNoMoreInteractions(writer);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void boolean_Fields_Deserialize_Correctly() throws Exception {
        boolean expected = true;
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aBooleanObject", expected)
                .createReader();
        reader.beginResponse();
        Booleans instance = transformer.getTypeAdapter(Booleans.class).deserialize(reader);
        assertEquals(instance.aBoolean, expected);
        assertEquals(instance.aBooleanObject, expected);
    }

    @Test
    public void boolean_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Booleans instance = transformer.getTypeAdapter(Booleans.class).deserialize(reader);
        assertFalse(instance.aBoolean);
        assertNull(instance.aBooleanObject);
    }

    private static class Booleans {
        @ParameterValue(PARAMETER_NAME)
        private boolean aBoolean;
        @ParameterValue
        private Boolean aBooleanObject;
    }

    /*
     * Byte
     * */

    @Test
    public void byte_Fields_Serialize_Correctly_For_Primitive_Values() throws IOException {
        Bytes object = new Bytes();
        byte expected = 0x32;
        object.aByte = expected;
        transformer.getTypeAdapter(Bytes.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeValue(eq((long) expected));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void byte_Fields_Serialize_Correctly_For_Boxed_Values() throws IOException {
        Bytes object = new Bytes();
        Byte expected = 0x32;
        object.aByte = expected;
        object.aByteObject = expected;
        transformer.getTypeAdapter(Bytes.class).serialize(writer, object);
        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName("aByteObject");
        verify(writer, times(2)).writeValue(eq(expected.longValue()));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void byte_Fields_Deserialize_Correctly() throws Exception {
        byte expected = 0x32;
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aByteObject", expected)
                .createReader();
        reader.beginResponse();
        Bytes instance = transformer.getTypeAdapter(Bytes.class).deserialize(reader);
        assertEquals(instance.aByte, expected);
        assertEquals(instance.aByteObject, Byte.valueOf(expected));
    }

    @Test
    public void byte_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Bytes instance = transformer.getTypeAdapter(Bytes.class).deserialize(reader);
        assertEquals(instance.aByte, 0);
        assertNull(instance.aByteObject);
    }

    private static class Bytes {
        @ParameterValue(PARAMETER_NAME)
        private byte aByte;
        @ParameterValue
        private Byte aByteObject;
    }

    /*
     * Short
     * */

    @Test
    public void short_Fields_Serialize_Correctly_For_Primitive_Values() throws IOException {
        Shorts object = new Shorts();
        short expected = Short.MAX_VALUE / 2;
        object.aShort = expected;
        transformer.getTypeAdapter(Shorts.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeValue(eq((long) expected));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void short_Fields_Serialize_Correctly_For_Boxed_Values() throws IOException {
        Shorts object = new Shorts();
        Short expected = Short.MAX_VALUE / 2;
        object.aShort = expected;
        object.aShortObject = expected;
        transformer.getTypeAdapter(Shorts.class).serialize(writer, object);
        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName("aShortObject");
        verify(writer, times(2)).writeValue(eq(expected.longValue()));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void short_Fields_Deserialize_Correctly() throws Exception {
        short expected = Short.MAX_VALUE / 2;
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aShortObject", expected)
                .createReader();
        reader.beginResponse();
        Shorts instance = transformer.getTypeAdapter(Shorts.class).deserialize(reader);
        assertEquals(instance.aShort, expected);
        assertEquals(instance.aShortObject, Short.valueOf(expected));
    }

    @Test
    public void short_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Shorts instance = transformer.getTypeAdapter(Shorts.class).deserialize(reader);
        assertEquals(instance.aShort, 0);
        assertNull(instance.aShortObject);
    }

    private static class Shorts {
        @ParameterValue(PARAMETER_NAME)
        private short aShort;
        @ParameterValue
        private Short aShortObject;
    }

    /*
     * Integer
     * */

    @Test
    public void integer_Fields_Serialize_Correctly_For_Primitive_Values() throws IOException {
        Integers object = new Integers();
        int expected = Integer.MAX_VALUE / 2;
        object.anInt = expected;
        transformer.getTypeAdapter(Integers.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeValue(eq((long) expected));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void integer_Fields_Serialize_Correctly_For_Boxed_Values() throws IOException {
        Integers object = new Integers();
        Integer expected = Integer.MAX_VALUE / 2;
        object.anInt = expected;
        object.anIntObject = expected;
        transformer.getTypeAdapter(Integers.class).serialize(writer, object);
        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName("anIntObject");
        verify(writer, times(2)).writeValue(eq(expected.longValue()));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void integer_Fields_Deserialize_Correctly() throws Exception {
        int expected = Integer.MAX_VALUE / 2;
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("anIntObject", expected)
                .createReader();
        reader.beginResponse();
        Integers instance = transformer.getTypeAdapter(Integers.class).deserialize(reader);
        assertEquals(instance.anInt, expected);
        assertEquals(instance.anIntObject, Integer.valueOf(expected));
    }

    @Test
    public void integer_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Integers instance = transformer.getTypeAdapter(Integers.class).deserialize(reader);
        assertEquals(instance.anInt, 0);
        assertNull(instance.anIntObject);
    }

    private static class Integers {
        @ParameterValue(PARAMETER_NAME)
        private int anInt;
        @ParameterValue
        private Integer anIntObject;
    }

    /*
     * Long
     * */

    @Test
    public void long_Fields_Serialize_Correctly_For_Primitive_Values() throws IOException {
        Longs object = new Longs();
        long expected = Long.MAX_VALUE / 2;
        object.aLong = expected;
        transformer.getTypeAdapter(Longs.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeValue(eq(expected));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void long_Fields_Serialize_Correctly_For_Boxed_Values() throws IOException {
        Longs object = new Longs();
        Long expected = Long.MAX_VALUE / 2;
        object.aLong = expected;
        object.aLongObject = expected;
        transformer.getTypeAdapter(Longs.class).serialize(writer, object);
        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName("aLongObject");
        verify(writer, times(2)).writeValue(eq(expected.longValue()));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void long_Fields_Deserialize_Correctly() throws Exception {
        long expected = Long.MAX_VALUE / 2;
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aLongObject", expected)
                .createReader();
        reader.beginResponse();
        Longs instance = transformer.getTypeAdapter(Longs.class).deserialize(reader);
        assertEquals(instance.aLong, expected);
        assertEquals(instance.aLongObject, Long.valueOf(expected));
    }

    @Test
    public void long_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Longs instance = transformer.getTypeAdapter(Longs.class).deserialize(reader);
        assertEquals(instance.aLong, 0L);
        assertNull(instance.aLongObject);
    }

    private static class Longs {
        @ParameterValue(PARAMETER_NAME)
        private long aLong;
        @ParameterValue
        private Long aLongObject;
    }

    /*
     * Float
     * */

    @Test
    public void float_Fields_Serialize_Correctly_For_Primitive_Values() throws IOException {
        Floats object = new Floats();
        float expected = Float.MAX_VALUE / 2.f;
        object.aFloat = expected;
        transformer.getTypeAdapter(Floats.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeValue(eq(expected));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void float_Fields_Serialize_Correctly_For_Boxed_Values() throws IOException {
        Floats object = new Floats();
        Float expected = Float.MAX_VALUE / 2.f;
        object.aFloat = expected;
        object.aFloatObject = expected;
        transformer.getTypeAdapter(Floats.class).serialize(writer, object);
        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName("aFloatObject");
        verify(writer, times(2)).writeValue(eq(expected.floatValue()));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void float_Fields_Deserialize_Correctly() throws Exception {
        float expected = Float.MAX_VALUE / 2.f;
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aFloatObject", expected)
                .createReader();
        reader.beginResponse();
        Floats instance = transformer.getTypeAdapter(Floats.class).deserialize(reader);
        assertEquals(instance.aFloat, expected, 0.d);
        assertEquals(instance.aFloatObject, Float.valueOf(expected));
    }

    @Test
    public void float_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Floats instance = transformer.getTypeAdapter(Floats.class).deserialize(reader);
        assertEquals(instance.aFloat, 0.f, 0.d);
        assertNull(instance.aFloatObject);
    }

    private static class Floats {
        @ParameterValue(PARAMETER_NAME)
        private float aFloat;
        @ParameterValue
        private Float aFloatObject;
    }

    /*
     * Double
     * */

    @Test
    public void double_Fields_Serialize_Correctly_For_Primitive_Values() throws IOException {
        Doubles object = new Doubles();
        double expected = Double.MAX_VALUE / 2.d;
        object.aDouble = expected;
        transformer.getTypeAdapter(Doubles.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeValue(eq(expected));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void double_Fields_Serialize_Correctly_For_Boxed_Values() throws IOException {
        Doubles object = new Doubles();
        Double expected = Double.MAX_VALUE / 2.d;
        object.aDouble = expected;
        object.aDoubleObject = expected;
        transformer.getTypeAdapter(Doubles.class).serialize(writer, object);
        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName("aDoubleObject");
        verify(writer, times(2)).writeValue(eq(expected.doubleValue()));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void double_Fields_Deserialize_Correctly() throws Exception {
        double expected = Double.MAX_VALUE / 2.d;
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aDoubleObject", expected)
                .createReader();
        reader.beginResponse();
        Doubles instance = transformer.getTypeAdapter(Doubles.class).deserialize(reader);
        assertEquals(instance.aDouble, expected, 0.d);
        assertEquals(instance.aDoubleObject, Double.valueOf(expected));
    }

    @Test
    public void double_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Doubles instance = transformer.getTypeAdapter(Doubles.class).deserialize(reader);
        assertEquals(instance.aDouble, 0.d, 0.d);
        assertNull(instance.aDoubleObject);
    }

    private static class Doubles {
        @ParameterValue(PARAMETER_NAME)
        private double aDouble;
        @ParameterValue
        private Double aDoubleObject;
    }

    /*
     * String
     * */

    @Test
    public void string_Fields_Serialize_Correctly_For_Non_Null_Values() throws IOException {
        Strings object = new Strings();
        String expected = "Some fancy value";
        object.aString = expected;
        object.aStringDefaultName = expected;
        transformer.getTypeAdapter(Strings.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName(eq("aStringDefaultName"));
        verify(writer, times(2)).writeValue(eq(expected));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void string_Fields_Serialize_Correctly_For_Null_Values() throws IOException {
        Strings object = new Strings();
        transformer.getTypeAdapter(Strings.class).serialize(writer, object);
        verifyZeroInteractions(writer);
    }

    @Test
    public void string_Fields_Deserialize_Correctly() throws Exception {
        String expected = "Pink Elephants";
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aStringDefaultName", expected)
                .createReader();
        reader.beginResponse();
        Strings instance = transformer.getTypeAdapter(Strings.class).deserialize(reader);
        assertEquals(instance.aString, expected);
        assertEquals(instance.aStringDefaultName, expected);
    }

    @Test
    public void string_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        Strings instance = transformer.getTypeAdapter(Strings.class).deserialize(reader);
        assertNull(instance.aString);
        assertNull(instance.aStringDefaultName);
    }

    private static class Strings {
        @ParameterValue(PARAMETER_NAME)
        private String aString;
        @ParameterValue
        private String aStringDefaultName;
    }

    /*
     * Non-Primitive Type
     * */

    @Test
    public void customType_Fields_Serialize_Correctly_Using_Provided_TypeAdapter() throws Exception {
        CustomType customType = new CustomType(Double.MAX_VALUE);
        transformer.getTypeAdapter(CustomType.class).serialize(writer, customType);

        verify(writer).writeValue(eq(customType.value));
        verifyNoMoreInteractions(writer);
        verify(typeAdapter).serialize(any(ProtocolWriter.class), refEq(customType));
    }

    @Test
    public void customType_Fields_Serialize_Correctly_For_Non_Null_Values() throws IOException {
        CustomTypes object = new CustomTypes();
        CustomType expected = new CustomType(Double.MAX_VALUE);
        object.aCustomType = object.aCustomTypeDefaultName = expected;
        transformer.getTypeAdapter(CustomTypes.class).serialize(writer, object);

        verify(writer).writeName(eq(PARAMETER_NAME));
        verify(writer).writeName(eq("aCustomTypeDefaultName"));
        verify(writer, times(2)).writeValue(eq(String.valueOf(expected.value)));
        verifyNoMoreInteractions(writer);
    }

    @Test
    public void customType_Fields_Serialize_Correctly_For_Null_Values() throws IOException {
        CustomTypes object = new CustomTypes();
        transformer.getTypeAdapter(CustomTypes.class).serialize(writer, object);
        verifyZeroInteractions(writer);
    }

    @Test
    public void customType_Fields_Deserialize_Correctly() throws Exception {
        String expected = "Pink Elephants";
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .writeValue(PARAMETER_NAME, expected)
                .writeValue("aCustomTypeDefaultName", expected)
                .createReader();
        reader.beginResponse();
        CustomTypes instance = transformer.getTypeAdapter(CustomTypes.class).deserialize(reader);
        assertEquals(instance.aCustomType.value, expected);
        assertEquals(instance.aCustomTypeDefaultName.value, expected);
    }

    @Test
    public void customType_Fields_Deserialize_Correctly_With_Missing_Values() throws Exception {
        ProtocolResponseReader reader = new ResponseBytesWriter()
                .createReader();
        reader.beginResponse();
        CustomTypes instance = transformer.getTypeAdapter(CustomTypes.class).deserialize(reader);
        assertNull(instance.aCustomType);
        assertNull(instance.aCustomTypeDefaultName);
    }

    private static class CustomTypeAdapter extends TypeAdapter<CustomType> {

        @Override
        public CustomType deserialize(ProtocolReader reader) throws IOException {
            return new CustomType(reader.readString());
        }

        @Override
        public void serialize(ProtocolWriter writer, CustomType value) throws IOException {
            writer.writeValue(value.value);
        }
    }

    private static class CustomTypes {
        @ParameterValue(PARAMETER_NAME)
        private CustomType aCustomType;
        @ParameterValue
        private CustomType aCustomTypeDefaultName;
    }

    private static class CustomType {
        private final String value;

        CustomType(Object object) {
            this(object != null ? String.valueOf(object) : null);
        }

        CustomType(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CustomType that = (CustomType) o;

            return value != null ? value.equals(that.value) : that.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }
}
