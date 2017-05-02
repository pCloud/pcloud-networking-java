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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A contract for an object able to execute many calls to a server and return the results
 * <p>
 * The request can be executed in a number of ways
 * <ul>
 * <li> {@linkplain #execute()} will synchronously execute all the calls one by one on
 * the same thread blocking until its done or an exception is thrown
 * <li> {@linkplain #enqueue(MultiCallback)} will asynchronously enqueue on another thread
 * calling a {@linkplain MultiCallback} to report progress, success or failure
 * <li> {@linkplain #enqueueAndWait()} will enqueue on another thread but will block the
 * calling thread until its done or an exception is thrown
 * <li> Can be used only once. You can check if this call has been executed via {@linkplain #isExecuted()}
 * <li> To reuse you can call {@linkplain #clone()} and execute the clone object
 * <li> It is safe to call {@linkplain #cancel()} multiple times
 * <p>
 * </ul>
 *
 * @param <T> The requests for this {@linkplain MultiCall}
 * @param <R> The results for this {@linkplain MultiCall}
 * @see MultiCallback
 */
public interface MultiCall<T, R> extends Cloneable {
    /**
     * Returns a {@linkplain List} of the requests for this {@linkplain MultiCall}
     *
     * @return A {@linkplain List} of the requests for this {@linkplain MultiCall}
     */
    List<T> requests();

    /**
     * Executes synchronously and blocks until completion or until an exception is thrown
     *
     * @return A {@linkplain List} of the result objects
     * @throws IOException on failed IO operations
     */
    List<R> execute() throws IOException;

    /**
     * Enqueues on another thread but blocks the calling thread until completion or until an exception is thrown
     *
     * @return A {@linkplain List} of the results from the requests
     * @throws IOException          on failed network operations
     * @throws InterruptedException if the calling thread is interrupted
     */
    List<R> enqueueAndWait() throws IOException, InterruptedException;

    /**
     * Enqueues on another thread but blocks the calling thread for at most the amount of time specified in the arguments.
     * <p>
     * Note you are not guaranteed to receive all the responses in the time you specified and
     * if all the operations are not complete in this time a {@linkplain TimeoutException} will be thrown
     *
     * @param timeout  The maximum amount of time before an attempt to collect the results from this operation
     * @param timeUnit The unit in which you specified the time argument
     * @return A {@linkplain List} containing the results from the network calls
     * @throws IOException          on failed IO operations
     * @throws InterruptedException if the waiting thread was interrupted
     * @throws TimeoutException     if the time is up before all the responses have been received
     */
    List<R> enqueueAndWait(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException, TimeoutException;

    /**
     * Enqueues asynchronously and calls a {@linkplain MultiCallback} to notify progress, completion or failure
     *
     * @param callback The {@linkplain MultiCallback} to be fired on success or failure
     */
    void enqueue(MultiCallback<T, R> callback);

    /**
     * Returns true if this {@linkplain MultiCall} has been executed already
     *
     * @return true if this {@linkplain MultiCall} has been executed already
     */
    boolean isExecuted();

    /**
     * Cancels all the requests in this {@linkplain MultiCall}
     */
    void cancel();

    /**
     * Returns true if this {@linkplain MultiCall} has been cancelled, false otherwise
     *
     * @return true if this {@linkplain MultiCall} has been cancelled, false otherwise
     */
    boolean isCancelled();

    /**
     * Creates a new instance of a {@linkplain MultiCall} object to be reused for the same calls
     *
     * @return A cloned new instance of a {@linkplain MultiCall} to make new requests
     */
    MultiCall<T, R> clone();
}