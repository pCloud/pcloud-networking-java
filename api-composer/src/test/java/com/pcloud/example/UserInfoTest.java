package com.pcloud.example;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Georgi on 4/21/2017.
 */
public class UserInfoTest extends AbstractTest {

    @Test
    public void getUserInfo_ShouldNotFail() throws Exception {

        UserInfoResponse response = userApi.getUserInfo(username, password, true);
        assertNotNull(response);
    }

    @Test
    public void getUserInfo_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        userApi.getUserInfo(null, "", true);
    }

    @Test
    public void getUserInfo1_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        UserInfoResponse response = userApi.getUserInfo1(request);
        assertNotNull(response);
    }

    @Test
    public void getUserInfo1_ShouldFailOnNullRequestObject() throws Exception {

        exception.expect(IllegalArgumentException.class);
        userApi.getUserInfo1(null);
    }

    @Test
    public void getUserInfo2_ShouldNotFail() throws Exception {

        com.pcloud.networking.Call<UserInfoResponse> call = userApi.getUserInfo2(username, password, true);
        UserInfoResponse response = call.execute();
        assertNotNull(response);
    }

    @Test
    public void getUserInfo2_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        com.pcloud.networking.Call<UserInfoResponse> call = userApi.getUserInfo2(null, null, false);
        call.execute();
    }

    @Test
    public void getUserInfo3_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        com.pcloud.networking.Call<UserInfoResponse> call = userApi.getUserInfo3(request);
        UserInfoResponse response = call.execute();
        assertNotNull(response);
    }

    @Test
    public void getUserInfo3_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        com.pcloud.networking.Call<UserInfoResponse> call = userApi.getUserInfo3(null);
        call.execute();
    }

    @Test
    public void getUserInfo4_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        com.pcloud.networking.MultiCall<UserInfoRequest, UserInfoResponse> call = userApi.getUserInfo4(request);
        List<UserInfoResponse> list = call.execute();
        assertNotNull(list);
        assertNotNull(list.get(0));
    }

    @Test
    public void getUserInfo4_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        userApi.getUserInfo4(null);
    }

    @Test
    public void getUserInfo5_ShouldNotFail() throws Exception {

        UserInfoRequest request = new UserInfoRequest(username, password, true);
        List<UserInfoRequest> list = new ArrayList<>();
        list.add(request);
        com.pcloud.networking.MultiCall<UserInfoRequest, UserInfoResponse> call = userApi.getUserInfo5(list);
        List<UserInfoResponse> response = call.execute();
        assertNotNull(response);
        assertNotNull(response.get(0));
    }

    @Test
    public void getUserInfo5_ShouldFailOnNullParameters() throws Exception {

        exception.expect(IllegalArgumentException.class);
        com.pcloud.networking.MultiCall<UserInfoRequest, UserInfoResponse> call = userApi.getUserInfo5(null);
        call.execute();
    }


}