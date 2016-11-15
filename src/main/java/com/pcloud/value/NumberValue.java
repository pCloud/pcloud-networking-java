package com.pcloud.value;

/**
 * Created by Georgi on 11/10/2016.
 */

public class NumberValue extends Value {

    protected long value;

    public NumberValue(long value) {
        this.value = value;
    }

    public final long getValue() {
        return value;
    }

    @Override
    public final boolean isNumber() {
        return true;
    }

    @Override
    public final long asNumber() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
