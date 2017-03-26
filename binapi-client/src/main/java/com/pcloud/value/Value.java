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

    public abstract boolean isPrimitive();
}
