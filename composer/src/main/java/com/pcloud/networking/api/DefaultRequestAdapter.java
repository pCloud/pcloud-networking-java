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

import com.pcloud.networking.client.Request;
import com.pcloud.networking.protocol.ProtocolWriter;

import java.io.IOException;

@SuppressWarnings("rawtypes")
class DefaultRequestAdapter implements RequestAdapter {

    private final ArgumentAdapter[] argumentAdapters;

    DefaultRequestAdapter(ArgumentAdapter[] argumentAdapters) {
        this.argumentAdapters = argumentAdapters;
    }

    @SuppressWarnings("unchecked")// ArgumentAdapters are already of the required type.
    @Override
    public void adapt(final Request.Builder requestBuilder, final Object... args) throws IOException {

        for (int index = 0; index < args.length; index++) {
            argumentAdapters[index].adapt(requestBuilder, args[index]);
        }

        requestBuilder.body(new com.pcloud.networking.client.RequestBody() {
            @Override
            public void writeTo(ProtocolWriter writer) throws IOException {
                for (int index = 0; index < args.length; index++) {
                    argumentAdapters[index].adapt(writer, args[index]);
                }
            }
        });
    }
}
