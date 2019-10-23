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

import java.io.IOException;

/**
 * A contract for a provider object able to provide an {@linkplain Endpoint}
 *
 * @see Endpoint
 */
public interface EndpointProvider {

    /**
     * Provides a default {@linkplain Endpoint}
     *
     * @see Endpoint#DEFAULT
     */
    EndpointProvider DEFAULT = new EndpointProvider() {
        @Override
        public Endpoint endpoint() {
            return Endpoint.DEFAULT;
        }

        @Override
        public void endpointConnectionError(Endpoint endpoint, IOException error) {
        }

        @Override
        public void endpointReadError(Endpoint endpoint, IOException error) {
        }

        @Override
        public void endpointWriteError(Endpoint endpoint, IOException error) {
        }
    };

    /**
     * Provides a custom {@linkplain Endpoint}
     *
     * @return The {@linkplain Endpoint} you'd like this provider to provide
     */
    Endpoint endpoint();

    /**
     * Called when an exception is thrown while connecting to the given {@linkplain Endpoint}
     * <p>
     *
     * @param endpoint The {@linkplain Endpoint} that had a connection error
     * @param error    The connection error
     */
    void endpointConnectionError(Endpoint endpoint, IOException error);

    /**
     * Called when an exception is thrown while reading from the given {@linkplain Endpoint}
     * <p>
     *
     * @param endpoint The {@linkplain Endpoint} that had a read error
     * @param error    The connection error
     */
    void endpointReadError(Endpoint endpoint, IOException error);

    /**
     * Called when an exception is thrown while writing to the given {@linkplain Endpoint}
     * <p>
     *
     * @param endpoint The {@linkplain Endpoint} that had a write error
     * @param error    The connection error
     */
    void endpointWriteError(Endpoint endpoint, IOException error);
}
