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

package com.pcloud.networking.client.internal.tls;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * A HostnameVerifier consistent with <a href="http://www.ietf.org/rfc/rfc2818.txt">RFC 2818</a>.
 */
public final class DefaultHostnameVerifier implements HostnameVerifier {
    public static final DefaultHostnameVerifier INSTANCE;

    static {
        //TODO: Switch over to the platform-provided implementations, where available.
        INSTANCE = new DefaultHostnameVerifier(OkHostnameVerifier.INSTANCE);
    }

    private HostnameVerifier platformHostnameVerifier;

    private DefaultHostnameVerifier(HostnameVerifier platformHostnameVerifier) {
        this.platformHostnameVerifier = platformHostnameVerifier;
    }

    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return platformHostnameVerifier.verify(s, sslSession);
    }
}
