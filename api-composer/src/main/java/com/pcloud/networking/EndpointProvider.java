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

import com.pcloud.Endpoint;

import java.io.IOException;

public interface EndpointProvider {

    EndpointProvider DEFAULT = new EndpointProvider() {
        @Override
        public Endpoint endpoint() {
            return Endpoint.DEFAULT;
        }

        @Override
        public void endpointConnectionError(Endpoint endpoint, IOException error) {

        }
    };

    Endpoint endpoint();

    void endpointConnectionError(Endpoint endpoint, IOException error);
}
