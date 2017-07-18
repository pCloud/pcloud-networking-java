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

package com.pcloud.networking.api;

import com.pcloud.utils.Types;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * An adapter that allows modifying the return type.
 * <p>
 * Install a implementation of {@link Factory} by calling
 * {@linkplain com.pcloud.networking.api.ApiComposer.Builder#addAdapterFactory(Factory)}
 * or
 * {@linkplain com.pcloud.networking.api.ApiComposer.Builder#addAdapterFactories(Iterable)}
 *
 * @param <T> The source type
 * @param <R> The adapted result type
 */
public interface CallAdapter<T, R> {

    /**
     * @return the actual type of {@linkplain R}
     */
    Type responseType();

    /**
     * Adapt a {@linkplain Call} to a {@linkplain R}
     *
     * @param call a non-null {@linkplain Call} instance
     * @return an adapted call of type {@linkplain R}
     * @throws IOException on a possible network failure
     */
    R adapt(Call<T> call) throws IOException;

    /**
     * Adapt a {@linkplain MultiCall} to a {@linkplain R}
     *
     * @param call a non-null {@linkplain MultiCall} instance
     * @return an adapted call of type {@linkplain R}
     * @throws IOException on a possible network failure
     */
    R adapt(MultiCall<?, T> call) throws IOException;

    /**
     * A factory class for creating {@linkplain CallAdapter} instances.
     */
    abstract class Factory {
        /**
         * Returns a {@linkplain CallAdapter} for interface methods that return {@code returnType}, or null if it
         * cannot be handled by this factory.
         *
         * @param apiComposer the calling {@linkplain ApiComposer} instance
         * @param method      the Java method to be adapted
         * @return a non-null {@linkplain CallAdapter} instanse
         */
        public abstract CallAdapter<?, ?> get(ApiComposer apiComposer, Method method);

        /**
         * Helper method for obtaining the actual type of a generic
         * type with multiple parameters
         *
         * @param index the index of the generic parameter
         * @param type the target generic type
         * @return the actual type of the parameter at {@code index}
         */
        protected static Type getParameterUpperBound(int index, Type type) {
            return Types.getParameterUpperBound(index, type);
        }
    }
}
