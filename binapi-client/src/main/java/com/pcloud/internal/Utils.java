/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.pcloud.internal;

import java.util.regex.Pattern;

/**
 * A utility class that checks to see if {@code host} is a host name or an IP address
 */
public class Utils {

    private static final Pattern VERIFY_AS_IP_ADDRESS = Pattern.compile(
            "([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");

    private Utils() {
    }

    /**
     * Checks to see if the {@code host} is host name or an IP address
     *
     * @param host The host name to be checked
     * @return true if {@code host} is not a host name and might be an IP address.
     */
    public static boolean verifyAsIpAddress(String host) {
        return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
    }
}
