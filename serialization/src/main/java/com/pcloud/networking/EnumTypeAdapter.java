package com.pcloud.networking;

import com.pcloud.protocol.streaming.ProtocolReader;
import com.pcloud.protocol.streaming.ProtocolWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Class<T> enumType;
    private final Map<String, T> nameToConstantMap;
    private final Map<T, String> constantToNameMap;

    public EnumTypeAdapter(Class<T> enumType) {
        this.enumType = enumType;
        try {
            T[]constants = enumType.getEnumConstants();
            this.nameToConstantMap = new HashMap<>(constants.length);
            this.constantToNameMap = new HashMap<>(constants.length);
            for (int i = 0; i < constants.length; i++) {
                T constant = constants[i];
                ParameterValue annotation = enumType.getField(constant.name()).getAnnotation(ParameterValue.class);
                String name = annotation != null && !annotation.value().equals(ParameterValue.DEFAULT_NAME) ?
                        annotation.value() : constant.name();
                nameToConstantMap.put(name, constant);
                constantToNameMap.put(constant, name);
            }
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field in " + enumType.getName(), e);
        }
    }



    @Override
    public T deserialize(ProtocolReader reader) throws IOException {
        String name = reader.readString();
        T enumConstant = nameToConstantMap.get(name);
        if (enumConstant != null){
            return enumConstant;
        } else {
            throw new IOException("Expected one of" + nameToConstantMap.keySet() + "but was '" + name + "'");
        }
    }

    @Override
    public void serialize(ProtocolWriter writer, T value) throws IOException {
        String name = constantToNameMap.get(value);
        writer.writeValue(name);
    }

    @Override public String toString() {
        return "JsonAdapter(" + enumType.getName() + ")";
    }
}