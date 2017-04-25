/*
 * Copyright (c) 2017 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.networking;

import com.pcloud.Request;
import com.pcloud.protocol.DataSource;
import com.pcloud.protocol.streaming.ProtocolWriter;
import com.pcloud.protocol.streaming.TypeToken;
import okio.ByteString;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

class ArgumentAdapters {

    private static abstract class BodyWritingArgumentAdapter<T> implements ArgumentAdapter<T> {

        @Override
        public void adapt(Request.Builder builder, T argValue) throws IOException {
            //Empty
        }
    }

    private static abstract class BuilderArgumentAdapter<T> implements ArgumentAdapter<T> {

        public void adapt(ProtocolWriter writer, T argValue) throws IOException {
            //Empty
        }
    }

    private ArgumentAdapters() {

    }

    static <T> ArgumentAdapter<T> parameter(final String name, final TypeAdapter<T> adapter) {
        return new BodyWritingArgumentAdapter<T>() {

            @Override
            public void adapt(ProtocolWriter writer, T argValue) throws IOException {

                if(argValue == null) {
                    throw new IllegalArgumentException("The parameter cannot be null");
                }
                writer.writeName(name);

                adapter.serialize(writer, argValue);
            }
        };
    }

    static <T> ArgumentAdapter<T> requestBody(final TypeAdapter<T> adapter) {
        return new BodyWritingArgumentAdapter<T>() {
            @Override
            public void adapt(ProtocolWriter writer, T argValue) throws IOException {
                if(argValue == null) {
                    throw new IllegalArgumentException("The RequestBody cannot be null");
                }
                adapter.serialize(writer, argValue);
            }
        };
    }

    static <T> ArgumentAdapter<T> dataSource(Type dataSourceType) {

        final Class<?> argType = Types.getRawType(dataSourceType);
        if (DataSource.class.isAssignableFrom(argType)) {
            return new BuilderArgumentAdapter<T>() {
                @Override
                public void adapt(Request.Builder builder, T argValue) throws IOException {
                    builder.dataSource((DataSource) argValue);
                }
            };
        } else if (File.class.isAssignableFrom(argType)) {
            return new BuilderArgumentAdapter<T>() {
                @Override
                public void adapt(Request.Builder builder, T argValue) throws IOException {
                    builder.dataSource(DataSource.create((File) argValue));
                }
            };
        } else if (ByteString.class.isAssignableFrom(argType)) {
            return new BuilderArgumentAdapter<T>() {
                @Override
                public void adapt(Request.Builder builder, T argValue) throws IOException {
                    builder.dataSource(DataSource.create((ByteString) argValue));
                }
            };
        } else if (byte[].class == argType) {
            return new BuilderArgumentAdapter<T>() {
                @Override
                public void adapt(Request.Builder builder, T argValue) throws IOException {
                    builder.dataSource(DataSource.create((byte[]) argValue));
                }
            };
        } else {
            throw new IllegalStateException("Cannot convert argument of type '" + argType + "' to DataSource.");
        }
    }
}
