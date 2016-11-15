package com.pcloud.value;

/**
 * Created by Georgi on 11/10/2016.
 */

public class BooleanValue extends Value {
    protected boolean value;

    public BooleanValue(boolean value) {
        this.value = value;
    }

    public final boolean getValue() {
        return value;
    }

    @Override
    public final boolean isBoolean() {
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
