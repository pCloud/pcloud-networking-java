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

import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;

/**
 * A contract for an interceptor which is able to intercept an network request and write some data to it
 *
 * @see Request
 * @see ProtocolWriter
 */
public interface RequestInterceptor {

    /**
     * Intercepts the {@linkplain Request} to write some data to it
     *
     * @param request The {@linkplain Request} to be intercepted
     * @param writer  The {@linkplain ProtocolWriter} to write the data with
     * @throws IOException on failed IO operations
     */
    void intercept(Request request, ProtocolWriter writer) throws IOException;
}
