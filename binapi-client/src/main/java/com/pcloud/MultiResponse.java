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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MultiResponse implements Closeable {

    private List<Response> responses;

    MultiResponse(List<Response> responses) {
        this.responses = Collections.unmodifiableList(responses);
    }

    public Request request(int index){
        return responses.get(index).request();
    }

    public Response response(int key){
        return responses.get(key);
    }

    public List<Response> responses(){
        return responses;
    }

    @Override
    public void close() throws IOException {
        for (Response r : responses) {
            r.close();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultiResponse response = (MultiResponse) o;

        return responses.equals(response.responses);

    }

    @Override
    public int hashCode() {
        return responses.hashCode();
    }

    @Override
    public String toString() {
        return responses.toString();
    }
}
