package com.pcloud;

import com.pcloud.value.ObjectValue;
import com.sun.istack.internal.Nullable;

/**
 * Created by Georgi on 11/10/2016.
 */

public class ApiResponse {

    private int resultCode;
    private String error;
    private ObjectValue responseBody;

    public ApiResponse(ObjectValue body) {
        this.responseBody = body;
        this.resultCode = (int) body.get("result").asNumber();
        if(!isSuccessful()) {
            this.error = body.get("error").asString();
        }
    }

    ObjectValue getResponseBody() {
        return responseBody;
    }

    public boolean isSuccessful() {
        return resultCode == 0;
    }

    public int getResultCode() {
        return resultCode;
    }

    @Nullable
    public String getErrorDescription() {
        return error;
    }

    @Override
    public String toString() {
        return responseBody.toString();
    }
}
