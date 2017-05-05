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

package com.pcloud.networking;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created by Dimitard on 3.5.2017 г..
 */
class DirectCallAdapter  {
    static final DirectCallAdapterFactory FACTORY = new DirectCallAdapterFactory();

    static class DirectCallAdapterFactory extends CallAdapter.Factory {
        @Override
        public CallAdapter<?, ?> get(ApiComposer apiComposer, Method method) {
            Type returnType = method.getGenericReturnType();
            if(!(returnType instanceof ApiResponse)) {
                return null;
            }
            return new CallAdapter<Object, Object>() {
                @Override
                public Type responseType() {
                    return null;
                }

                @Override
                public Object adapt(Call<Object> call) throws IOException {
                    return call.execute();
                }

                @Override
                public Object adapt(MultiCall<?, Object> call) throws IOException {
                    return call.execute();
                }
            };
        }
    }
}
