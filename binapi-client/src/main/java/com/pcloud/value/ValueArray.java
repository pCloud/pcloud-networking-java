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

package com.pcloud.value;

import java.util.Iterator;
import java.util.List;

public class ValueArray extends Value implements Iterable<Value> {
    private List<Value> values;

    public ValueArray(List<Value> values) {
        this.values = values;
    }

    @Override
    public final boolean isArray() {
        return true;
    }

    @Override
    public final ValueArray asArray() {
        return this;
    }

    @Override
    public final boolean isPrimitive() {
        return false;
    }

    public final Value get(int position) {
        return values.get(position);
    }

    @Override
    public final Iterator<Value> iterator() {
        return values.iterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        Iterator<Value> iterator = values.iterator();
        boolean hasMore = iterator.hasNext();
        while (hasMore) {
            builder.append(iterator.next().toString());
            if (hasMore = iterator.hasNext()) {
                builder.append(',');
            }
            if (prettyPrintEnabled) {
                builder.append("\n");
            }
        }
        builder.append(']');
        return builder.toString();
    }
}
