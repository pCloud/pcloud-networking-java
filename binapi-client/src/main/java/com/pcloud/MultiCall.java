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

/**
 * Created by Georgi on 26.12.2016 г..
 */
public interface MultiCall extends Cloneable {
    List<Request> requests();

    MultiResponse execute() throws IOException;

    MultiResponse enqueueAndWait() throws IOException, InterruptedException;

    MultiResponse enqueueAndWait(long timeout, TimeUnit timeUnit) throws IOException, InterruptedException, TimeoutException;

    void enqueue(MultiCallback callback);

    boolean isExecuted();

    void cancel();

    boolean isCancelled();

    MultiCall clone();
}
