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
 * A contract for a callback
 * <p>
 * The callback is to be fired upon completion of a {@linkplain Call} or upon an exception.
 * Generally used for network calls.
 *
 * @see Call
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface Callback {

    /**
     * Fired when the {@linkplain Call} fails due to an {@linkplain IOException}
     *
     * @param call The {@linkplain Call} during which an {@linkplain IOException} was thrown
     * @param e    The {@linkplain IOException} that was thrown during execution of the {@linkplain Call}
     */
    void onFailure(Call call, IOException e);

    /**
     * Fired when a {@linkplain Response} is received after a successful execution of a {@linkplain Call}
     *
     * @param call     The {@linkplain Call} that was executed
     * @param response The {@linkplain Response} of the {@linkplain Call}
     * @throws IOException on failed IO operations
     */
    void onResponse(Call call, Response response) throws IOException;
}
