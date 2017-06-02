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
import java.util.Collections;
import java.util.List;

import static com.pcloud.IOUtils.closeQuietly;

/**
 * An implementation of a container for multiple responses.
 * <p>
 * Stores and provides all the {@linkplain Request} and {@linkplain Response} objects
 * for the {@linkplain MultiCall} that returned this response.
 *
 * @see MultiCall
 * @see Request
 * @see Response
 */
public class MultiResponse implements Closeable {

    private List<Response> responses;

    MultiResponse(List<Response> responses) {
        this.responses = Collections.unmodifiableList(responses);
    }


    /**
     * Returns the {@linkplain Request} for a given index
     *
     * @param index the index of the {@linkplain Request} you'd like to get
     * @return A reference to the {@linkplain Request} at a given index
     */
    public Request request(int index) {
        return responses.get(index).request();
    }


    /**
     * Returns a {@linkplain Response} for the position of the {@linkplain Response}
     * in numerical order corresponding to the position of the {@linkplain Request} in numerical order
     *
     * @param key The number of the {@linkplain Response}
     *            which corresponds to the numerical order in which the requests were made
     * @return The {@linkplain Response} which corresponds to the {@linkplain Request} with the same number in numerical order
     */
    public Response response(int key) {
        return responses.get(key);
    }

    /**
     * Returns a {@linkplain List} of all the {@linkplain Response} objects within the {@linkplain MultiResponse}
     *
     * @return A {@linkplain List} of all the {@linkplain Response} objects in this {@linkplain MultiResponse}
     */
    public List<Response> responses() {
        return responses;
    }

    /**
     * Closes all the responses in this {@linkplain MultiResponse}
     */
    @Override
    public void close() {
        for (Response r : responses) {
            closeQuietly(r);
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
