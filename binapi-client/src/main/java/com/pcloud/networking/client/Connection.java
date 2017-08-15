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

package com.pcloud.networking.client;

import okio.BufferedSink;
import okio.BufferedSource;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

interface Connection extends Closeable {

    Endpoint endpoint();

    BufferedSource source() throws IOException;

    BufferedSink sink() throws IOException;

    InputStream inputStream() throws IOException;

    OutputStream outputStream() throws IOException;

    void readTimeout(long timeout, TimeUnit timeUnit) throws IOException;

    void writeTimeout(long timeout, TimeUnit timeUnit) throws IOException;

    int readTimeout();

    int writeTimeout();

    @Override
    void close();
}
