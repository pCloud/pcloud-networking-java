/*
 * Copyright (C) 2017 pCloud AG.
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

package com.pcloud.networking.serialization;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Controls whether a field should be serialized to/deserialised from binary protocol values.
 * <p>
 * The {@linkplain #value()} property allows control over the name to be used during transformation
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface ParameterValue {

    String DEFAULT_NAME = " _%DEFAULT";

    /**
     * The parameter name to be used when serializing/deserializing from
     * binary protocol form.
     */
    String value() default DEFAULT_NAME;
}
