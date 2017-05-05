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

package com.pcloud.networking;

import com.pcloud.protocol.streaming.TypeToken;

import java.lang.reflect.Type;

class Util {

    static TypeToken getParameterType(Type type) {
        if (type == Long.class || type == long.class ||
                type == Integer.class || type == int.class ||
                type == Short.class || type == short.class ||
                type == Byte.class || type == byte.class ||
                type == Double.class || type == double.class ||
                type == Float.class || type == float.class) {
            return TypeToken.NUMBER;
        } else if (type == String.class || Types.getRawType(type).isEnum()) {
            return TypeToken.STRING;
        } else if (type == Boolean.class || type == boolean.class) {
            return TypeToken.BOOLEAN;
        }

        return null;
    }
}
