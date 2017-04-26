package com.pcloud.example;

import com.pcloud.*;
import com.pcloud.networking.ApiComposer;
import com.pcloud.networking.Transformer;
import com.pcloud.protocol.streaming.ProtocolWriter;
import com.pcloud.protocol.streaming.TypeToken;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.pcloud.IOUtils.closeQuietly;

/**
 * Created by Georgi on 4/24/2017.
 */
public abstract class AbstractTest {

    protected static PCloudAPIClient apiClient;
    protected static Transformer transformer;
    protected static ApiComposer apiComposer;
    protected static FileOperationApi donwloadApi;
    protected static FolderApi folderApi;
    protected static UserApi userApi;

    protected static String username;
    protected static String password;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws Exception {

        Map<String, String> env = System.getenv();
        username = env.get("pcloud_username");
        password = env.get("pcloud_password");

        apiClient = PCloudAPIClient.newClient()
                .addInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(Request request, ProtocolWriter writer) throws IOException {
                        writer.writeName("timeformat").writeValue("timestamp");
                    }
                })
                .create();
        transformer = Transformer.create().build();

        final String token = getAuthToken(apiClient, transformer, username, password);
        apiClient = apiClient.newBuilder().addInterceptor(new RequestInterceptor() {
            @Override
            public void intercept(Request request, ProtocolWriter writer) throws IOException {
                writer.writeName("auth").writeValue(token);
            }
        }).create();


        apiComposer = ApiComposer.create()
                .apiClient(apiClient)
                .transformer(transformer)
                .loadEagerly(true)
                .create();

        donwloadApi = apiComposer.compose(FileOperationApi.class);
        folderApi = apiComposer.compose(FolderApi.class);
        userApi = apiComposer.compose(UserApi.class);
    }

    private static String getAuthToken(PCloudAPIClient client, Transformer transformer, String username, String password) throws IOException, InterruptedException {
        Map<String, Object> values = new HashMap<>();
        values.put("getauth", 1);
        values.put("username", username);
        values.put("password", password);

        Response response = null;
        try {
            response = client.newCall(Request.create()
                    .methodName("userinfo")
                    .body(RequestBody.fromValues(values))
                    .build())
                    .enqueueAndWait();
            UserInfoResponse apiResponse = transformer.getTypeAdapter(UserInfoResponse.class)
                    .deserialize(response.responseBody().reader());
            return apiResponse.authenticationToken();
        } finally {
            closeQuietly(response);
        }
    }
}
