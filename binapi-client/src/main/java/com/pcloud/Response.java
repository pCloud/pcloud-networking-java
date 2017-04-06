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

public class Response implements Closeable {

    public static class Builder {

        private ResponseBody responseBody;
        private Request request;

        private Builder(){

        }

        public Builder request(Request request) {
            if (request == null) {
                throw new IllegalArgumentException("Request argument cannot be null.");
            }
            this.request = request;
            return this;
        }

        public Builder responseBody(ResponseBody responseBody) {
            if (responseBody == null) {
                throw new IllegalArgumentException("ResponseBody argument cannot be null.");
            }

            this.responseBody = responseBody;
            return this;
        }

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

    public static Builder create() {
        return new Builder();
    }

    private Response(Builder builder) {
        this.responseBody = builder.responseBody;
        this.request = builder.request;
    }

    public Request request() {
        return request;
    }

    public ResponseBody responseBody() {
        return responseBody;
    }

    public String toString() {
        return responseBody.toString();
    }

    @Override
    public void close() {
        closeQuietly(responseBody);
    }
}
