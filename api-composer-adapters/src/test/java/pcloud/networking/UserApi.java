/*
 * Copyright (c) 2017 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pcloud.networking;

import com.pcloud.networking.Call;
import com.pcloud.networking.Method;
import com.pcloud.networking.MultiCall;
import com.pcloud.networking.Parameter;
import com.pcloud.networking.RequestBody;
import com.pcloud.networking.UserInfoRequest;
import com.pcloud.networking.UserInfoResponse;
import rx.Observable;

import java.io.IOException;
import java.util.List;

public interface UserApi {

    @Method("userinfo")
    Observable<UserInfoResponse> getUserInfo(@Parameter("username")String username, @Parameter("password")String password, @Parameter("getauth") boolean returnToken) throws IOException;

    @Method("userinfo")
    Observable<UserInfoResponse> getUserInfo2(@RequestBody UserInfoRequest request);

    @Method("userinfo")
    Observable<UserInfoResponse> getUserInfo3(@RequestBody List<UserInfoRequest> request);

    @Method("userinfo")
    Observable<UserInfoResponse> getUserInfo4(@RequestBody UserInfoRequest... requests);
}
