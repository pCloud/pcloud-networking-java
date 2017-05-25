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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.pcloud.IOUtils.closeQuietly;

/**
 * Created by Georgi on 26.12.2016 г..
 */

/**
 * A contract for an object which can handle multiple calls
 * <p>
 * A {@linkplain MultiCall} object can execute several calls one after another.
 * {@linkplain MultiCall} objects can and should be reused via the {@linkplain #clone()} method.
 * <p>
 * Note that all {@linkplain Request} objects to add to a {@linkplain MultiCall}
 * should be opted for the same {@linkplain Endpoint}
 *
 * @see MultiCallback
 * @see Call
 * @see Response
 * @see MultiResponse
 * @see Request
 */
public interface MultiCall extends Cloneable {


    /**
     * Returns a {@linkplain List} of the {@linkplain Request} objects in this {@linkplain MultiCall}
     *
     * @return A {@linkplain List} of the {@linkplain Request} objects for this {@linkplain MultiCall}
     */
    List<Request> requests();

    /**
     * Executes the calls on the same thread and blocks it until the operation is complete or an exception is thrown.
     *
     * @return A reference to the {@linkplain MultiResponse} of this call
     * @throws IOException On failed IO operations
     */
    MultiResponse execute() throws IOException;

    Interactor start() throws IOException;

    /**
     * Executes the calls on another thread but blocks the calling thread until the operation is complete or an exception is thrown.
     *
     * @return A reference to the {@linkplain MultiResponse} of this call
     * @throws IOException          On failed IO operations
     * @throws InterruptedException On interruption of the waiting thread
     */
    MultiResponse enqueueAndWait() throws IOException, InterruptedException;

    /**
     * Executes the calls on another thread but blocks the calling thread for at most the duration specified in the arguments.
     * <p>
     * Note that it is not guaranteed that all the calls will execute in time and
     * you  are not guaranteed to receive a {@linkplain MultiResponse} within the time you specified.
     *
     * @param timeout  The maximum amount of waiting time before an attempt to collect and return a {@linkplain MultiResponse}
     * @param timeUnit The {@linkplain TimeUnit} in which you have specified the waiting time
     * @return A reference to the {@linkplain MultiResponse} of this call
     * @throws IOException          On failed IO operations
     * @throws InterruptedException On interruption of the waiting thread
     * @throws TimeoutException     If all the calls have not been executed prior to the maximum time specified running out
     */
    MultiResponse enqueueAndWait(long timeout, TimeUnit timeUnit)
            throws IOException, InterruptedException, TimeoutException;

    /**
     * Executes the call on another thread and does not block the calling thread
     *
     * @param callback A {@linkplain MultiCallback} to be fired upon a successful execution of a call,
     *                 upon an exception being thrown or upon completion of all the calls
     */
    void enqueue(MultiCallback callback);

    /**
     * Returns true if the all the calls have been executed already, false otherwise
     *
     * @return true if all the calls have been executed already, false otherwise
     */
    boolean isExecuted();

    /**
     * Cancels all the calls within this {@linkplain MultiCall}
     * <p>
     * Note that once cancelled a {@linkplain MultiCall} can not be executed properly again unless it is cloned first.
     */
    void cancel();

    /**
     * Returns true if this {@linkplain MultiCall} has been cancelled, false otherwise
     *
     * @return true if this {@linkplain MultiCall} has been cancelled, false otherwise
     */
    boolean isCancelled();

    /**
     * Clones this {@linkplain MultiCall} object into a new instance with all the same parameters in order for it to be reused.
     *
     * @return A reference to a new instance of a {@linkplain MultiCall} object with the same parameters as this one.
     */
    MultiCall clone();
}
