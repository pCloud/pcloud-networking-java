package com.pcloud.networking.serialization;

import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;


/**
 * Created by Radoslav
 * on 03-Jan-18.
 */
public class ArrayTypeAdapterTest {

    private static final String expectedResult = "1,1,1";
    private ProtocolWriter writer;

    @SuppressWarnings("unchecked")
    private static final TypeAdapter<Object> INT_TYPE_ADAPTER =
            (TypeAdapter<Object>)
                    (TypeAdapter<?>) PrimitiveTypesAdapterFactory.INTEGER_ADAPTER;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        writer = mock(ProtocolWriter.class);
    }

    @After
    public void tearDown() {
        writer = null;
    }


    @Test
    public void skips_Writing_When_Serializing_Null_Array() throws Exception {
        ArrayTypeAdapter typeAdapter = new ArrayTypeAdapter(int.class, INT_TYPE_ADAPTER);
        typeAdapter.serialize(writer, null);
        verifyZeroInteractions(writer);
    }

    @Test
    public void returns_Correct_Constant_When_Serializing_IntArray() throws Exception {
        int[] array = new int[]{1, 1, 1};
        ArrayTypeAdapter typeAdapter = new ArrayTypeAdapter(int.class, INT_TYPE_ADAPTER);
        typeAdapter.serialize(writer, array);
        verify(writer).writeValue(expectedResult);
    }

    @Test
    public void returns_Correct_Constant_When_Serializing_StringArray() throws Exception {
        String[] array = new String[]{"1", "1", "1"};
        ArrayTypeAdapter typeAdapter = new ArrayTypeAdapter(String.class, PrimitiveTypesAdapterFactory.STRING_ADAPTER);
        typeAdapter.serialize(writer, array);
        verify(writer).writeValue(expectedResult);
    }

    @Test
    public void returns_Correct_Constant_When_Serializing_IntArrayList() throws Exception {
        List<Integer> list = Arrays.asList(1, 1, 1);
        CollectionTypeAdapter<List<Integer>, Integer> adapter = new CollectionTypeAdapter<List<Integer>, Integer>
                (PrimitiveTypesAdapterFactory.INTEGER_ADAPTER) {
            @Override
            protected List<Integer> instantiateCollection() {
                return new ArrayList<>();
            }
        };
        adapter.serialize(writer, list);
        verify(writer).writeValue(expectedResult);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void returns_Correct_Constant_When_Serializing_CustomObjectArrayList() throws Exception {
        CustomObject object = new CustomObject();
        List<CustomObject> list = Arrays.asList(object, object, object);
        CollectionTypeAdapter<List<CustomObject>, CustomObject> adapter = new CollectionTypeAdapter<List<CustomObject>, CustomObject>(CUSTOM_OBJECT_ADAPTER) {
            @Override
            protected List<CustomObject> instantiateCollection() {
                return new ArrayList<>();
            }
        };
        adapter.serialize(writer, list);
        verify(writer).writeValue(expectedResult);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void returns_Correct_Constant_When_Serializing_CustomObjectArrayList_With_ExtraNull() throws Exception {
        CustomObject object = new CustomObject();
        List<CustomObject> list = Arrays.asList(object, object, null, object);
        CollectionTypeAdapter<List<CustomObject>, CustomObject> adapter = new CollectionTypeAdapter<List<CustomObject>, CustomObject>(CUSTOM_OBJECT_ADAPTER) {
            @Override
            protected List<CustomObject> instantiateCollection() {
                return new ArrayList<>();
            }
        };
        adapter.serialize(writer, list);
        verify(writer).writeValue(expectedResult);
    }

    private static final TypeAdapter CUSTOM_OBJECT_ADAPTER = new TypeAdapter<CustomObject>() {

        @Override
        public CustomObject deserialize(ProtocolReader reader) throws IOException {
            throw new IllegalStateException();
        }

        @Override
        public void serialize(ProtocolWriter writer, CustomObject value) throws IOException {
            if (value != null) {
                writer.writeValue(value.getField());
            }
        }
    };

    private static final class CustomObject {

        String getField() {
            return "1";
        }
    }
}