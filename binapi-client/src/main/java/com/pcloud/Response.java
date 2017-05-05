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

import java.io.Closeable;

import static com.pcloud.IOUtils.closeQuietly;

/**
 * An implementation of a response to a network request
 *
 * @see Request
 * @see ResponseBody
 */
public class Response implements Closeable {

    /**
     * A builder to construct {@linkplain Response} objects
     */
    public static class Builder {

        private ResponseBody responseBody;
        private Request request;

        private Builder() {

        }

        /**
         * Sets the {@linkplain Request} that was executed to receive this {@linkplain Response}
         *
         * @param request The {@linkplain Request} that was executed to receive this {@linkplain Response}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain Request} argument
         */
        public Builder request(Request request) {
            if (request == null) {
                throw new IllegalArgumentException("Request argument cannot be null.");
            }
            this.request = request;
            return this;
        }

        /**
         * Sets the {@linkplain ResponseBody} for this {@linkplain Response}
         *
         * @param responseBody The {@linkplain ResponseBody} to be set to this {@linkplain Response}
         * @return A reference to the {@linkplain Builder} object
         * @throws IllegalArgumentException on a null {@linkplain ResponseBody} argument
         */
        public Builder responseBody(ResponseBody responseBody) {
            if (responseBody == null) {
                throw new IllegalArgumentException("ResponseBody argument cannot be null.");
            }

            this.responseBody = responseBody;
            return this;
        }

        /**
         * Builds and returns the {@linkplain Response} with all the parameters set via the {@linkplain Builder}
         *
         * @return A reference to a new instance of a {@linkplain Response} object
         * constructed with all the parameters set via the {@linkplain Builder}
         * @throws IllegalArgumentException on a null {@linkplain Request} or a null {@linkplain ResponseBody} arguments
         */
        public Response build() {
            if (request == null) {
                throw new IllegalArgumentException("Request argument cannot be null.");
            }
            if (responseBody == null) {
                throw new IllegalArgumentException("ResponseBody argument cannot be null.");
            }

            return new Response(this);
        }
    }

    private ResponseBody responseBody;
    private Request request;

    /**
     * Creates and returns a new instance of the {@linkplain Builder} to construct a new {@linkplain Response}
     *
     * @return A reference to a new instance of the {@linkplain Builder} object to construct a new {@linkplain Response}
     */
    public static Builder create() {
        return new Builder();
    }

    private Response(Builder builder) {
        this.responseBody = builder.responseBody;
        this.request = builder.request;
    }

    /**
     * Returns the {@linkplain Request} for this {@linkplain Response}
     *
     * @return A reference to the {@linkplain Request} for this {@linkplain Response}
     */
    public Request request() {
        return request;
    }

    /**
     * Returns the {@linkplain ResponseBody} of this {@linkplain Response}
     *
     * @return A reference to the {@linkplain ResponseBody} of this {@linkplain Response}
     */
    public ResponseBody responseBody() {
        return responseBody;
    }

    /**
     * Returns a String representation of the {@linkplain Response}
     *
     * @return A String repsentation of the {@linkplain Response}
     * @see ResponseBody#toString()
     */
    public String toString() {
        return responseBody.toString();
    }

    @Override
    public void close() {
        closeQuietly(responseBody);
    }
}
