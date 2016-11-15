package com.pcloud.value;

/**
 * Created by Georgi on 11/10/2016.
 */

public class StringValue extends Value {
    protected String value;

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public final boolean isString() {
        return true;
    }

    @Override
    public final String asString() {
        return value;
    }

    public final String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
