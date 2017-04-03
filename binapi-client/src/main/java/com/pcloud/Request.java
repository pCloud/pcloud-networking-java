/*
 * Copyright (c) 2016 Georgi Neykov
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

import com.pcloud.protocol.DataSource;

public class Request {

    public static Builder create() {
        return new Builder();
    }

    private String methodName;
    private RequestBody body;
    private DataSource dataSource;

    private Request(Builder builder) {
        this.methodName = builder.methodName;
        this.body = builder.body;
        this.dataSource = builder.dataSource;
    }

    public String methodName() {
        return methodName;
    }

    public RequestBody body() {
        return body;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public Builder newRequest() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return String.format("Method:\'%s\', hasData=%s\n%s", methodName, dataSource!= null, body.toString());
    }

    public static class Builder {
        private String methodName;
        private RequestBody body;
        private DataSource dataSource;

        private Builder(){}

        private Builder(Request request) {
            methodName = request.methodName;
            body = request.body;
            dataSource = request.dataSource;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder dataSource(DataSource source) {
            this.dataSource = source;
            return this;
        }

        public Builder body(RequestBody body) {
            this.body = body;
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }
}
