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

package com.pcloud.networking.serialization;

import java.lang.reflect.Type;

/**
 * A contract for a factory able to create {@linkplain TypeAdapter} objects
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public interface TypeAdapterFactory {

    /**
     * Creates an instance of a {@linkplain TypeAdapter} object
     *
     * @param type        The data type that the {@linkplain TypeAdapter} will work with
     * @param transformer The {@linkplain Transformer} to create the {@linkplain TypeAdapter}
     * @return A {@linkplain TypeAdapter} that works with the {@linkplain Type} argument
     */
    TypeAdapter<?> create(Type type, Transformer transformer);
}
