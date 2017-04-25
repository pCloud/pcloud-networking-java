package com.pcloud;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static com.pcloud.IOUtils.closeQuietly;
import static org.junit.Assert.assertEquals;

/**
 * Created by Georgi on 4/24/2017.
 */
public class BinApiTest {

    private static String username;
    private static String password;

    private static PCloudAPIClient cloudAPIClient;
    private static String accessToken;

    @BeforeClass
    public static void setUp() throws Exception{

        username = System.getenv("pcloud_username");
        password = System.getenv("pcloud_password");

        cloudAPIClient = PCloudAPIClient.newClient()
                .create();
        accessToken = getAuth();
    }

    @Test
    public void callsRequestShouldBeEqualToResponsesRequest() throws Exception {


        Map<String, Object> values = new TreeMap<>();
        values.put("folderid", 0L);

        Request request =  Request.create()
                .body(RequestBody.fromValues(values))
                .methodName("listfolder")
                .build();

        Call call;
        Response response = null;
        try {
            call = cloudAPIClient.newCall(request);
            response = call.execute();

            response.responseBody().toValues();

            assertEquals(request, response.request());
        } finally {
            closeQuietly(response);
        }
    }

    private static String getAuth() throws Exception{

        Map<String, Object> values = new TreeMap<>();
        values.put("getauth", 1);
        values.put("username", username);
        values.put("password", password);

        Call call;
        Response response = null;
        String accessToken;
        try {
            Request request = Request.create()
                    .body(RequestBody.fromValues(values))
                    .methodName("userinfo")
                    .build();

            call = cloudAPIClient.newCall(request);
            response = call.execute();
            Map<String, ?> map;
            map = response.responseBody().toValues();
            accessToken = (String) map.get("auth");

        } finally {
            closeQuietly(response);
        }

        return accessToken;
    }
}
