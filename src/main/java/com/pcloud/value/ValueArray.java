package com.pcloud.value;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Georgi on 11/10/2016.
 */

public class ValueArray extends Value implements Iterable<Value>{
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
            if(prettyPrintEnabled) {
                builder.append("\n");
            }
        }
        builder.append(']');
        return builder.toString();
    }
}
