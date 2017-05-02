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

import java.io.IOException;
import java.util.List;

/**
 * A contact for a callback to work with a {@linkplain MultiCall}
 *
 * @param <T> The request type for the {@linkplain MultiCall}
 * @param <R> The response type for the {@linkplain MultiCall}
 * @see MultiCall
 */
public interface MultiCallback<T, R> {

    /**
     * Fires once only if an exception is thrown during the execution of one of the requests
     *
     * @param call               A reference {@linkplain MultiCall} object within which one of the calls failed
     * @param e                  The {@linkplain IOException} which was thrown
     * @param completedResponses A {@linkplain List} of all the responses that were completed prior to the exception
     */
    void onFailure(MultiCall<T, R> call, IOException e, List<R> completedResponses);

    /**
     * Called each time when a response has been received for one of the requests
     *
     * @param call     A reference for the {@linkplain MultiCall} for which one
     *                 of the requests has been completed successfully
     * @param key      The number of the response which corresponds to
     *                 the number of the request in numerical order
     * @param response The response that was received
     */
    void onResponse(MultiCall<T, R> call, int key, R response);

    /**
     * Fires once only after all the requests were executed successfully and all the responses were received
     *
     * @param call    The {@linkplain MultiCall} which was executed
     * @param results A {@linkplain List} of the results of all the calls
     */
    void onComplete(MultiCall<T, R> call, List<R> results);
}
