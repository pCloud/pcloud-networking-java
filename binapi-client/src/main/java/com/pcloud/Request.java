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

import com.pcloud.value.*;

public class Request {

    public static Builder create() {
        return new Builder();
    }

    private String methodName;
    private ObjectValue requestParameters;
    private Data data;

    private Request(Builder builder) {
        this.methodName = builder.methodName;
        this.requestParameters = builder.requestParameters;
        this.data = builder.data;
    }

    public String methodName() {
        return methodName;
    }

    public ObjectValue requestParameters() {
        return requestParameters;
    }

    public Data data() {
        return data;
    }

    public Builder newRequest() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return String.format("Method:\'%s\', hasData=%s\n%s", methodName, data!= null, requestParameters);
    }

    public static class Builder {
        private String methodName;
        private ObjectValue requestParameters = new ObjectValue();
        private Data data;

        private Builder(){}

        private Builder(Request request) {
            methodName = request.methodName;
            requestParameters = request.requestParameters;
            data = request.data;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder addParameter(String name, PrimitiveValue value) {
            requestParameters.put(name, value);
            return this;
        }

        public Builder addParameter(String name, String value) {
            return addParameter(name, new StringValue(value));
        }

        public Builder addParameter(String name, boolean value) {
            return addParameter(name, new BooleanValue(value));
        }

        public Builder addParameter(String name, int value) {
            return addParameter(name, new NumberValue(value));
        }

        public Builder addParameter(String name, long value) {
            return addParameter(name, new NumberValue(value));
        }

        public Builder addParameter(String name, byte value) {
            return addParameter(name, new NumberValue(value));
        }

        public Builder removeParameter(String name) {
            requestParameters.remove(name);
            return this;
        }

        public Builder data(Data data) {
            this.data = data;
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }
}
