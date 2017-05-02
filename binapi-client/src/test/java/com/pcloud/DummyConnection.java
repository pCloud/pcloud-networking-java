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

package com.pcloud;


import okio.*;

import static org.mockito.Mockito.mock;

/**
 * Created by Sajuuk-khar on 13.4.2017 г..
 */
public class DummyConnection implements Connection {

    private Buffer readBuffer;
    private Buffer writeBuffer;
    private Endpoint endpoint;

    public DummyConnection(Endpoint endpoint, byte[] data) {
        this.endpoint = endpoint;
        this.readBuffer = new Buffer();
        this.writeBuffer = new Buffer();
        readBuffer.write(data);
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }

    @Override
    public BufferedSource source() {
        return readBuffer;
    }

    @Override
    public BufferedSink sink() {
        return writeBuffer;
    }

    @Override
    public void close() {
    }
}
