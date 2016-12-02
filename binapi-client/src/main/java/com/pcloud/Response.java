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

import com.pcloud.value.ObjectValue;
import com.sun.istack.internal.Nullable;

import java.io.Closeable;
import java.io.IOException;

public class Response implements Closeable {

    private int resultCode;
    private String error;
    private ObjectValue responseBody;
    private ResponseData data;

    public Response(ObjectValue body, ResponseData data) {
        this.responseBody = body;
        this.data = data;
        this.resultCode = (int) body.get("result").asNumber();
        if(!isSuccessful()) {
            this.error = body.get("error").asString();
        }
    }

    public ObjectValue values() {
        return responseBody;
    }

    public ResponseData data() {
        return data;
    }

    public boolean hasBinaryData() {
        return data != null;
    }

    public boolean isSuccessful() {
        return resultCode == 0;
    }

    public int resultCode() {
        return resultCode;
    }

    public String errorDescription() {
        return error;
    }

    public String toString() {
        return responseBody.toString();
    }

    @Override
    public void close() {
        if(data != null) {
            data.close();
        }
    }
}
