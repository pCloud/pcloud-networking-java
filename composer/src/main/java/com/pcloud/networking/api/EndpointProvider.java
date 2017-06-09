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
    };

    /**
     * Provides a custom {@linkplain Endpoint}
     *
     * @return The {@linkplain Endpoint} you'd like this provider to provide
     */
    Endpoint endpoint();

    /**
     * Called when an exception is thrown on the current {@linkplain Endpoint}
     * <p>
     * You should switch to the {@linkplain #DEFAULT} here
     *
     * @param endpoint The {@linkplain Endpoint} that has an exception
     * @param error    The exception that was thrown on the {@linkplain Endpoint}
     */

    void endpointConnectionError(Endpoint endpoint, IOException error);
}
