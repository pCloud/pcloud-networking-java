/*
 * Copyright (c) 2020 pCloud AG
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

class Protocol {

    static final int TYPE_STRING_START = 0;
    static final int TYPE_STRING_END = 3;
    static final int TYPE_STRING_REUSED_START = 4;
    static final int TYPE_STRING_REUSED_END = 7;
    static final int TYPE_NUMBER_START = 8;
    static final int TYPE_NUMBER_END = 15;
    static final int TYPE_BEGIN_OBJECT = 16;
    static final int TYPE_BEGIN_ARRAY = 17;
    static final int TYPE_BOOLEAN_FALSE = 18;
    static final int TYPE_BOOLEAN_TRUE = 19;
    static final int TYPE_DATA = 20;
    static final int TYPE_STRING_COMPRESSED_START = 100;
    static final int TYPE_STRING_COMPRESSED_END = 149;
    static final int TYPE_STRING_COMPRESSED_REUSED_BEGIN = 150;
    static final int TYPE_STRING_COMPRESSED_REUSED_END = 199;
    static final int TYPE_NUMBER_COMPRESSED_START = 200;
    static final int TYPE_NUMBER_COMPRESSED_END = 219;
    static final int TYPE_END_ARRAY_OBJECT = 255;

    static final int SIZE_RESPONSE_LENGTH = 4;
    static final int SIZE_DATA_BYTESIZE = 8;

    private Protocol() {
        throw new UnsupportedOperationException();
    }


    static String scopeName(final int scope) {
        switch (scope) {
            case ProtocolResponseReader.SCOPE_RESPONSE:
                return "Response";
            case ProtocolReader.SCOPE_ARRAY:
                return "Array";
            case ProtocolReader.SCOPE_OBJECT:
                return "Object";
            default:
                return "Unknown";
        }
    }
}
