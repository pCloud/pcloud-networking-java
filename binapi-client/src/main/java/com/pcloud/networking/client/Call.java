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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A contract for a network call to a server
 * @see Request
 * @see Response
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface Call extends Cloneable {

    /**
     * Provides the {@linkplain Request} of this call
     *
     * @return The {@linkplain Request} of this call
     */
    Request request();

    /**
     * Executes the call.
     * <p>
     * Note this method executes on the thread on which it's called an blocks until it's done or an exception is thrown.
     *
     * @return The {@linkplain Response} of the call
     * @throws IOException on failed IO operations
     */
    Response execute() throws IOException;

    /**
     * Enqueues the call on another thread and blocks the calling thread
     * until the {@linkplain Response} is received or an exception is thrown.
     *
     * @return The {@linkplain Response} of the call
     * @throws IOException          on failed IO operations
     * @throws InterruptedException if the thread is interrupted during execution
     */
    Response enqueueAndWait() throws IOException, InterruptedException;

    /**
     * Enqueues the call on another thread and blocks the calling thread
     * for at most the given time period after
     * which provides the {@linkplain Response} if ready.
     * <p>
     * Note that you are not guaranteed to receive the expected
     * {@linkplain Response} within the time period specified.
     *
     * @param timeout  The maximum period of time after which the {@linkplain Call} should try to retrieve the result
     * @param timeUnit The unit in which you've provided the time period
     * @return The {@linkplain Response} if the computation is completed within the time period specified
     * @throws IOException          on failed IO operations
     * @throws InterruptedException on interruption of the computation
     * @throws TimeoutException     if the {@linkplain Response} was not received within the time period
     */
    Response enqueueAndWait(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException, TimeoutException;

    /**
     * Enqueues the {@linkplain Call} on another thread.
     * <p>
     * {@linkplain Callback#onResponse(Call, Response)} will be called if all goes well
     * {@linkplain Callback#onFailure(Call, IOException)} will be called in case there was an exception during the call
     *
     * @param callback The {@linkplain Callback} to be fired upon completion or an error.
     */
    void enqueue(Callback callback);

    /**
     * Returns a boolean to show if the {@linkplain Call} has been executed already.
     * <p>
     * Note that a {@linkplain Call} can only be executed once
     *
     * @return true if the {@linkplain Call} has been executed already, false otherwise
     */
    boolean isExecuted();

    /**
     * Cancels this {@linkplain Call}.
     * <p>
     * Note that once cancelled a {@linkplain Call} will not be executable properly after that.
     */
    void cancel();

    /**
     * Returns a boolean to show if the {@linkplain Call} has been cancelled already.
     *
     * @return true if the {@linkplain Call} has been cancelled, false otherwise
     */
    boolean isCancelled();

    /**
     * Clones the current {@linkplain Call} into a new instance with the same parameters and returns it.
     *
     * @return A new instance of a {@linkplain Call} with the same parameters
     */
    Call clone();
}
