package com.pcloud.networking.serialization;

import com.pcloud.utils.Types;

import java.lang.reflect.Type;

class Utils {

    private Utils() {

    }

    static boolean isAllowedPlatformType(Type type) {
        return type == Boolean.class ||
                type == Byte.class ||
                type == Double.class ||
                type == Float.class ||
                type == Integer.class ||
                type == Long.class ||
                type == Short.class ||
                type == String.class;
    }

    static boolean fieldTypeIsSerializable(Type type) {
        return type == Long.class || type == long.class ||
                type == Integer.class || type == int.class ||
                type == Short.class || type == short.class ||
                type == Byte.class || type == byte.class ||
                type == Double.class || type == double.class ||
                type == Float.class || type == float.class ||
                type == String.class || Types.getRawType(type).isEnum() ||
                type == Boolean.class || type == boolean.class;
    }
}
