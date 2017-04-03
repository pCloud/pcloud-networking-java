package com.pcloud.networking;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public interface TypeAdapterFactory {

    TypeAdapter<?> create(Type type, Set<? extends Annotation> annotations, Cyclone cyclone);
}
