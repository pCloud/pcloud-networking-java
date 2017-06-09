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

import java.io.Closeable;
import java.io.IOException;

/**
 * A Iterator-like interface allowing the execution of {@link MultiCall}s in a pull-based fashion.
 * <p>
 * Using this class when executing {@link MultiCall} instances gives more control over connection writes and reads, providing the following benefits:
 * <ul>
 * <li>The number of simultaneous request writes can be controlled.</li>
 * <li>Responses can be read in an iterative fashion, avoiding the need for buffering.</li>
 * <li>Reading can start before all requests have been written.</li>
 * <li>The execution can be stopped in the middle of the process, with decisions on each received response.</li>
 * </ul>
 * <p>
 * <b>NOTE:Take care of calling {@linkplain #close()} when done with objects of this type, or resource leaks will occur.</b>
 */
public interface Interactor<T> extends Closeable {

    /**
     * @return {@code true} if there are more requests that can be written, {@code false} otherwise.
     */
    boolean hasMoreRequests();

    /**
     * Write a given amount of requests.
     * <p>
     * When called, the method will block and attempt to send at most {@code count} requests, but no more than the remaining request count left to be sent.
     * The returned value is the actual number of requests sent.
     * <p>
     * For an example, if the total request count is 5, {@code submitRequests(6)} will return {@code 5}.
     * Next calls to {@code submitRequests()} will always return {@code 0}.
     *
     * @param count the amount of requests to be sent
     * @return the actual number of sent requests
     * @throws IllegalArgumentException when {@code count < 0}
     * @throws IOException              if an error occurs during sending, or the {@linkplain MultiCall} has been cancelled.
     */

    int submitRequests(int count) throws IOException;

    /**
     * @return {@code true} if there are more {@linkplain T}s that can be read, {@code false} otherwise.
     */
    boolean hasNextResponse();

    /**
     * Read the next response
     * <p>
     * When called, the method will block and attempt to read a single response, either returning the result or failing with an error.
     * <p>
     * The method will throw an {@linkplain IllegalStateException} if the number of sent requests is equal to the number of received responses
     * to avoid the case where the method will be waiting for a response, but none is expected to arrive, thus blocking forever (or a connection read timeout is reached).
     *
     * @return the next {@linkplain T}
     * @throws IllegalStateException if trying to read more responses than sent requests
     * @throws IOException           if an error occurs during reading, or the {@linkplain MultiCall} has been cancelled.
     */
    T nextResponse() throws IOException;

    @Override
    void close();
}
