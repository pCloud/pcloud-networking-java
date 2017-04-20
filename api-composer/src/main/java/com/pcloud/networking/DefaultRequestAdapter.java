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
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;

class DefaultRequestAdapter implements RequestAdapter{

    private ArgumentAdapter[] argumentAdapters;

    DefaultRequestAdapter(ArgumentAdapter[] argumentAdapters) {
        this.argumentAdapters = argumentAdapters;
    }

    @Override
    public void adapt(final Request.Builder requestBuilder, final Object... args) {
        requestBuilder.body(new com.pcloud.RequestBody() {
            @SuppressWarnings("unchecked")
            @Override
            public void writeТо(ProtocolWriter writer) throws IOException {
                for (int index = 0; index < args.length; index++) {
                    argumentAdapters[index].adapt(requestBuilder, writer, args[index]);
                }
            }
        });
    }
}
