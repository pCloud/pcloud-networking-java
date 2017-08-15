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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

abstract class BaseConnection implements Connection {

    private int readTimeout;
    private int writeTimeout;

    @Override
    public void readTimeout(long timeout, TimeUnit timeUnit) throws IOException {
        readTimeout = (int) timeUnit.toMillis(timeout);
        BufferedSource source = source();
        if (source != null) {
            source.timeout().timeout(readTimeout, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void writeTimeout(long timeout, TimeUnit timeUnit) throws IOException {
        writeTimeout = (int) timeUnit.toMillis(timeout);
        BufferedSink sink = sink();
        if (sink != null) {
            sink.timeout().timeout(writeTimeout, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public int readTimeout() {
        return writeTimeout;
    }

    @Override
    public int writeTimeout() {
        return readTimeout;
    }
}
