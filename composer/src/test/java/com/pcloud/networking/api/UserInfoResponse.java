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

package com.pcloud.networking.api;

import com.pcloud.networking.serialization.ParameterValue;

public class UserInfoResponse extends ApiResponse {

    private boolean cryptosetup;

    @ParameterValue("plan")
    private int plan;
    private boolean cryptosubscription;
    @ParameterValue("email")
    private String email;
    private boolean emailverified;
    @ParameterValue("quota")
    private long quota;
    private long publiclinkquota;
    private boolean premium;
    private long userid;
    @ParameterValue("usedquota")
    private long usedquota;
    @ParameterValue("auth")
    private String authenticationToken;

    private UserInfoResponse() {}

    public UserInfoResponse(long result, String message) {
        super(result, message);
    }

    public boolean cryptosetup() {
        return cryptosetup;
    }

    public int plan() {
        return plan;
    }

    public boolean cryptosubscription() {
        return cryptosubscription;
    }

    public String email() {
        return email;
    }

    public boolean emailverified() {
        return emailverified;
    }

    public long quota() {
        return quota;
    }

    public long publiclinkquota() {
        return publiclinkquota;
    }

    public boolean premium() {
        return premium;
    }

    public long userid() {
        return userid;
    }

    public long usedquota() {
        return usedquota;
    }

    public String authenticationToken() {
        return authenticationToken;
    }
}
