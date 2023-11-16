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

package com.pcloud.networking.protocol;

/**
 * Enumeration of constant values representing different types when reading data from a source
 */
public enum TypeToken {
    NUMBER("Number"),
    STRING("String"),
    BOOLEAN("Boolean"),
    BEGIN_ARRAY("Begin Array"),
    BEGIN_OBJECT("Begin Object"),
    END_OBJECT("End Object"),
    END_ARRAY("End Array");

    private final String name;

    TypeToken(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
