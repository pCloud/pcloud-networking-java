package com.pcloud.example;

import com.pcloud.networking.ParameterValue;

public class UserInfoResponse extends ApiResponse {

    private boolean cryptosetup;
    private int plan;
    private boolean cryptosubscription;
    private String email;
    private boolean emailverified;
    private long quota;
    private long publiclinkquota;
    private boolean premium;
    private long userid;
    private long usedquota;
    @ParameterValue("auth")
    private String authenticationToken;

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
