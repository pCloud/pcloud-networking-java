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

package com.pcloud.networking.api;

import com.pcloud.networking.client.Endpoint;
import com.pcloud.networking.client.Request;
import com.pcloud.networking.protocol.DataSource;
import com.pcloud.networking.protocol.ProtocolWriter;
import com.pcloud.networking.serialization.GuardedSerializationTypeAdapter;
import com.pcloud.networking.serialization.TypeAdapter;
import com.pcloud.utils.Types;
import okio.ByteString;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

class ArgumentAdapters {

    private abstract static class BodyWritingArgumentAdapter<T> implements ArgumentAdapter<T> {

        @Override
        public void adapt(Request.Builder builder, T argValue) throws IOException {
            //Empty
        }
    }

    private abstract static class BuilderArgumentAdapter<T> implements ArgumentAdapter<T> {

        public void adapt(ProtocolWriter writer, T argValue) throws IOException {
            //Empty
        }
    }

    private ArgumentAdapters() {
        throw new UnsupportedOperationException();
    }

    static <T> ArgumentAdapter<T> parameter(final String name, final TypeAdapter<T> adapter) {

        final TypeAdapter<T> guardedTypeAdapter = new GuardedSerializationTypeAdapter<>(adapter);

        return new BodyWritingArgumentAdapter<T>() {

            @Override
            public void adapt(ProtocolWriter writer, T argValue) throws IOException {
                if (argValue != null) {
                    writer.writeName(name);
                    guardedTypeAdapter.serialize(writer, argValue);
                }
            }
        };
    }

    static <T> ArgumentAdapter<T> requestBody(final TypeAdapter<T> adapter) {
        return new BodyWritingArgumentAdapter<T>() {
            @Override
            public void adapt(ProtocolWriter writer, T argValue) throws IOException {
                if (argValue == null) {
                    throw new IllegalArgumentException("The RequestBody parameter cannot be null.");
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
                    if (argValue == null) {
                        throw new IllegalArgumentException("The RequestData parameter cannot be null.");
                    }
                    builder.dataSource((DataSource) argValue);
                }
            };
        } else if (File.class.isAssignableFrom(argType)) {
            return new BuilderArgumentAdapter<T>() {
                @Override
                public void adapt(Request.Builder builder, T argValue) throws IOException {
                    if (argValue == null) {
                        throw new IllegalArgumentException("The RequestData parameter cannot be null.");
                    }
                    builder.dataSource(DataSource.create((File) argValue));
                }
            };
        } else if (ByteString.class.isAssignableFrom(argType)) {
            return new BuilderArgumentAdapter<T>() {
                @Override
                public void adapt(Request.Builder builder, T argValue) throws IOException {
                    if (argValue == null) {
                        throw new IllegalArgumentException("The RequestData parameter cannot be null.");
                    }
                    builder.dataSource(DataSource.create((ByteString) argValue));
                }
            };
        } else if (byte[].class == argType) {
            return new BuilderArgumentAdapter<T>() {
                @Override
                public void adapt(Request.Builder builder, T argValue) throws IOException {
                    if (argValue == null) {
                        throw new IllegalArgumentException("The RequestData parameter cannot be null.");
                    }
                    builder.dataSource(DataSource.create((byte[]) argValue));
                }
            };
        } else {
            throw new IllegalStateException("Cannot convert argument of type '" + argType + "' to DataSource.");
        }
    }

    static ArgumentAdapter<Endpoint> endpoint() {
        return new BuilderArgumentAdapter<Endpoint>() {
            @Override
            public void adapt(Request.Builder builder, Endpoint argValue) {
                builder.endpoint(argValue);
            }
        };
    }
}
