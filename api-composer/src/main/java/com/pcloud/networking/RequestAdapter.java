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

import com.pcloud.Request;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A contract for an object able to adapt arguments to a request using a {@linkplain Request.Builder}
 */
interface RequestAdapter {
    /**
     * A factory for {@linkplain RequestAdapter} construction
     */
    interface Factory {
        RequestAdapter create(ApiComposer composer, java.lang.reflect.Method method, Type[] argumentTypes, Annotation[][] argumentAnnotations);
    }

    /**
     * Takes the arguments for the request and invokes the builder to construct a request with these arguments
     *
     * @param builder The builder to be invoked to construct the request
     * @param args    The request arguments for the builder to work with
     * @throws IOException on failed IO operations
     */
    void adapt(Request.Builder builder, Object... args) throws IOException;
}
