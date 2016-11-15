package com.pcloud.value;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Georgi on 11/10/2016.
 */

public class ObjectValue extends Value {
    private final Map<String, Value> properties = new TreeMap<String, Value>();

    @Override
    public final boolean isObject() {
        return true;
    }

    @Override
    public final ObjectValue asObject() {
        return this;
    }

    public final void put(String name, Value value) {
        properties.put(name, value);
    }

    public final void putString(String name, String value) {
        properties.put(name, new StringValue(value));
    }

    public final void putNumber(String name, long value) {
        properties.put(name, new NumberValue(value));
    }

    public final void putBoolean(String name, boolean value) {
        properties.put(name, new BooleanValue(value));
    }

    public final Value get(String name) {
        return properties.get(name);
    }

    public final Set<Map.Entry<String, Value>> propertySet(){
        return properties.entrySet();
    }

    public final boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        @SuppressWarnings("unchecked")
        Iterator<Map.Entry<String, Value>> i = properties.entrySet().iterator();
        boolean hasMore = i.hasNext();
        while (hasMore) {
            Map.Entry<String, Value> entry = i.next();
            builder.append("\"").append(entry.getKey()).append("\"")
                    .append(':')
                    .append(entry.getValue().toString());
            if (hasMore = i.hasNext()) {
                builder.append(',');
            }
            if(prettyPrintEnabled) {
                builder.append("\n");
            }
        }
        builder.append('}');
       return builder.toString();
    }
}
