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

package com.pcloud.networking.api;

import com.pcloud.networking.client.ConnectionPool;
import com.pcloud.networking.client.PCloudAPIClient;
import com.pcloud.networking.protocol.ProtocolReader;
import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.protocol.SerializationException;
import com.pcloud.networking.serialization.Transformer;
import com.pcloud.networking.serialization.TypeAdapter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MethodParametersTest {

    private static final Charset TEST_CHARSET = Charset.forName("UTF-8");
    private static final Date TEST_DATE = new Date(0);
    
    private static final Date[] DATE_ARRAY = {TEST_DATE, TEST_DATE, TEST_DATE};
    private static final Collection<Date> DATE_COLLECTION = Arrays.asList(TEST_DATE, TEST_DATE, TEST_DATE);

    private static final Charset[] CHARSET_ARRAY = {TEST_CHARSET, TEST_CHARSET, TEST_CHARSET};
    private static final Collection<Charset> CHARSET_COLLECTION = Arrays.asList(TEST_CHARSET, TEST_CHARSET, TEST_CHARSET);
    
    private static ParameterApi api;
    private static PCloudAPIClient apiClient;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUpClass() throws Exception {
        apiClient = PCloudAPIClient.newClient()
                .connectionPool(new ConnectionPool(2, 30, TimeUnit.SECONDS))
                .create();
        Transformer transformer = Transformer.create()
                .addTypeAdapter(Charset.class, new CharsetTypeAdapter())
                .addTypeAdapter(Date.class, new DateTypeAdapter())
                .build();
        api = ApiComposer.create()
                .apiClient(apiClient)
                .transformer(transformer)
                .loadEagerly(true)
                .create()
                .compose(ParameterApi.class);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        apiClient.shutdown();
    }



    @Test
    public void API_does_throw_For_SingleValue_Object() throws Exception {
        expectedException.expect(IOException.class);
        api.objectParameter(TEST_DATE);
    }

    @Test
    public void API_does_throw_For_SingleValue_Object_Array() throws Exception {
        expectedException.expect(IOException.class);
        api.objectArrayParameter(DATE_ARRAY);
    }

    @Test
    public void API_does_throw_For_SingleValue_Object_Collection() throws Exception {
        expectedException.expect(IOException.class);
        api.objectCollectionParameter(DATE_COLLECTION);
    }



    @Test
    public void API_throws_SerializationException_For_MultiValue_Object() throws Exception {
        expectedException.expect(SerializationException.class);
        api.invalidObjectParameter(TEST_CHARSET);
    }

    @Test
    public void API_throws_SerializationException_For_MultiValue_Object_Array() throws Exception {
        expectedException.expect(SerializationException.class);
        api.invalidObjectArrayParameter(CHARSET_ARRAY);
    }

    @Test
    public void API_throws_SerializationException_For_MultiValue_Object_Collection() throws Exception {
        expectedException.expect(SerializationException.class);
        api.invalidObjectCollectionParameter(CHARSET_COLLECTION);
    }



    private static class CharsetTypeAdapter extends TypeAdapter<Charset> {

        @Override
        public Charset deserialize(ProtocolReader reader) throws IOException {
            return null;
        }

        @Override
        public void serialize(ProtocolWriter writer, Charset value) throws IOException {
            writer.writeValue(value.displayName())
                    .writeName("isregistered").writeValue(value.isRegistered());
        }

    }
    private static class DateTypeAdapter extends TypeAdapter<Date> {

        @Override
        public Date deserialize(ProtocolReader reader) throws IOException {
            return null;
        }

        @Override
        public void serialize(ProtocolWriter writer, Date value) throws IOException {
            writer.writeValue(value.getTime() / 1000L);
        }

    }

    private interface ParameterApi {

        @Method("nonexistent")
        ApiResponse objectParameter(@Parameter("date") Date date) throws IOException;

        @Method("nonexistent")
        ApiResponse objectArrayParameter(@Parameter("dates") Date[] dates) throws IOException;

        @Method("nonexistent")
        ApiResponse objectCollectionParameter(@Parameter("items") Collection<Date> dates) throws IOException;


        @Method("nonexistent")
        ApiResponse invalidObjectParameter(@Parameter("charset") Charset charset) throws IOException;

        @Method("nonexistent")
        ApiResponse invalidObjectArrayParameter(@Parameter("charsets") Charset[] charsets) throws IOException;

        @Method("nonexistent")
        ApiResponse invalidObjectCollectionParameter(@Parameter("charsets") Collection<Charset> charsets) throws IOException;


        @Method("nonexistent")
        ApiResponse enumsCollectionParameter(@Parameter("units") Collection<TimeUnit> units) throws IOException;

        @Method("nonexistent")
        ApiResponse enumsArrayParameter(@Parameter("units") TimeUnit[] units) throws IOException;

        @Method("nonexistent")
        ApiResponse enumParameter(@Parameter("units") TimeUnit unit) throws IOException;
    }
}
