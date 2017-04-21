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

package com.pcloud;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings({"WeakerAccess", "unused"})
public interface Call extends Cloneable {
    Request request();

    Response execute() throws IOException;

    Response enqueueAndWait() throws IOException, InterruptedException;

    Response enqueueAndWait(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException, TimeoutException;

    void enqueue(Callback callback);

    boolean isExecuted();

    void cancel();

    boolean isCancelled();

    Call clone();
}
