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

import com.pcloud.networking.protocol.DataSource;

/**
 * An implementation of a network request to the server
 *
 * @see Call
 * @see MultiCall
 * @see PCloudAPIClient
 * @see RequestBody
 * @see Endpoint
 */
public class Request {
    /**
     * Creates a new instance of a {@linkplain Builder} to build the {@linkplain Request}
     *
     * @return A new instance of a {@linkplain Builder} to build the {@linkplain Request}
     */
    public static Builder create() {
        return new Builder();
    }

    private Endpoint endpoint;
    private String methodName;
    private RequestBody body;
    private DataSource dataSource;

    Request(Builder builder) {
        this.methodName = builder.methodName;
        this.body = builder.body;
        this.dataSource = builder.dataSource;
        this.endpoint = builder.endpoint;
    }

    /**
     * Returns the method name for this request
     *
     * @return A {@linkplain String} with the name of the method of this {@linkplain Request}
     */
    public String methodName() {
        return methodName;
    }

    /**
     * Returns the {@linkplain RequestBody} of this {@linkplain Request}
     *
     * @return A reference to the {@linkplain RequestBody} of this {@linkplain Request}
     */
    public RequestBody body() {
        return body;
    }

    /**
     * Returns the {@linkplain DataSource} of this {@linkplain Request}
     *
     * @return A reference to the {@linkplain DataSource} of this {@linkplain Request}
     */
    public DataSource dataSource() {
        return dataSource;
    }

    /**
     * Returns the {@linkplain Endpoint} of this {@linkplain Request}
     *
     * @return A reference to the {@linkplain Endpoint} of this {@linkplain Request}
     */
    public Endpoint endpoint() {
        return endpoint;
    }

    /**
     * Returns a new {@linkplain Builder} to construct a new {@linkplain Request}
     *
     * @return A reference to new instance of the {@linkplain Builder} to build a new {@linkplain Request}
     */
    public Builder newRequest() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return String.format("Method:\'%s\', hasData=%s\n%s", methodName, dataSource != null, body.toString());
    }

    /**
     * A builder to build a {@linkplain Request} with and set all its parameters
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static class Builder {
        private String methodName;
        private RequestBody body;
        private DataSource dataSource;
        private Endpoint endpoint = Endpoint.DEFAULT;

        private Builder() {
        }

        private Builder(Request request) {
            methodName = request.methodName;
            body = request.body;
            dataSource = request.dataSource;
            endpoint = request.endpoint;
        }

        /**
         * Sets the method name for the {@linkplain Request}
         *
         * @param methodName The method name to be set to the {@linkplain Request}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null method name argument
         */
        public Builder methodName(String methodName) {
            if (methodName == null) {
                throw new IllegalArgumentException("Method name cannot be null.");
            }

            this.methodName = methodName;
            return this;
        }

        /**
         * Sets the {@linkplain DataSource} for the {@linkplain Request}
         *
         * @param source The {@linkplain DataSource} to be set to the {@linkplain Request}
         * @return A reference to the {@linkplain Builder} object
         */
        public Builder dataSource(DataSource source) {
            this.dataSource = source;
            return this;
        }

        /**
         * Sets the {@linkplain RequestBody} for the {@linkplain Request}
         *
         * @param body The {@linkplain RequestBody} to be set to the {@linkplain Request}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain RequestBody} argument
         */
        public Builder body(RequestBody body) {
            if (body == null) {
                throw new IllegalArgumentException("RequestBody argument cannot be null.");
            }

            this.body = body;
            return this;
        }

        /**
         * Sets the {@linkplain Endpoint} for the {@linkplain Request}
         *
         * @param endpoint The {@linkplain Endpoint} to be set to the {@linkplain Request}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain Endpoint} argument
         */
        public Builder endpoint(Endpoint endpoint) {
            if (endpoint == null) {
                throw new IllegalArgumentException("Endpoint argument cannot be null.");
            }
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Build and returns a new {@linkplain Request} with the parameters set via the {@linkplain Builder}
         *
         * @return A new instance of a {@linkplain Request} with the {@linkplain Builder} parameters set
         */
        public Request build() {
            return new Request(this);
        }
    }
}
