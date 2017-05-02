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

package com.pcloud;

import java.io.IOException;
import java.util.List;

/**
 * A contract for a callback to be fired when making a {@linkplain MultiCall}
 *
 * @see MultiCall
 * @see Response
 * @see Request
 * @see MultiResponse
 */
public interface MultiCallback {

    /**
     * Fires only once when one of the calls execution has thrown an {@linkplain IOException}
     * <p>
     * Note that the rest of the calls scheduled for execution will
     * not be executed and {@linkplain #onComplete(MultiCall, MultiResponse)} will not be called.
     *
     * @param call               A reference to the {@linkplain MultiCall} object.
     * @param e                  The {@linkplain IOException} that was thrown during execution.
     * @param completedResponses A {@linkplain List} of the {@linkplain Response} objects that were constructed
     *                           from successful call executions prior to the {@linkplain IOException}
     */
    void onFailure(MultiCall call, IOException e, List<Response> completedResponses);

    /**
     * Fires each time a request is successful.
     *
     * @param call     A reference to the {@linkplain MultiCall} object
     * @param key      The position in numerical order of the {@linkplain Response} which corresponds
     *                 to the position of the {@linkplain Request} in numerical order
     * @param response A reference to the {@linkplain Response} obtained from the request
     * @throws IOException On failed IO operations
     */
    void onResponse(MultiCall call, int key, Response response) throws IOException;

    /**
     * Fires once only after all the requests have been executed and only if no exceptions were thrown in the process.
     * <p>
     * Note that {@linkplain #onComplete(MultiCall, MultiResponse)}
     * will not fire if {@linkplain #onFailure(MultiCall, IOException, List)} does.
     *
     * @param call     A reference to the {@linkplain MultiCall} object
     * @param response A reference to the {@linkplain MultiResponse} object
     * @throws IOException On failed IO operations
     */
    void onComplete(MultiCall call, MultiResponse response) throws IOException;
}
