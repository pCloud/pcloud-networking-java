package com.pcloud.value;

/**
 * Created by Georgi on 11/10/2016.
 */

public abstract class Value {

    static volatile boolean prettyPrintEnabled = true;

    public boolean isString(){
        return (this instanceof StringValue);
    }

    public boolean isNumber(){
        return (this instanceof NumberValue);
    }

    public boolean isBoolean(){
        return (this instanceof BooleanValue);
    }

    public boolean isObject(){
        return (this instanceof ObjectValue);
    }

    public boolean isArray(){
        return (this instanceof ValueArray);
    }

    public ObjectValue asObject(){
        throw new IllegalStateException("Not an object value: " + this);
    }

    public ValueArray asArray() {
        throw new IllegalStateException("Not an array value: " + this);
    }

    public String asString(){
        throw new IllegalStateException("Not a string value: " + this);

    }

    public boolean asBoolean(){
        throw new IllegalStateException("Not an boolean value: " + this);

    }

    public long asNumber(){
        throw new IllegalStateException("Not a number value: " + this);
    }
}
