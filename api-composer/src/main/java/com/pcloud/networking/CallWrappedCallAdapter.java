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

import java.lang.reflect.*;
import java.lang.reflect.Method;

import static com.pcloud.networking.Types.getRawType;

/**
 * Created by Dimitard on 3.5.2017 г..
 */
class CallWrappedCallAdapter {
    static final CallWrappedCallAdapterFactory FACTORY = new CallWrappedCallAdapterFactory();

    static class CallWrappedCallAdapterFactory extends CallAdapter.Factory {

        @Override
        public CallAdapter<?, ?> get(ApiComposer apiComposer, Method method) {
            Type returnType = method.getGenericReturnType();
            Class<?> rawType = getRawType(returnType);
            final Type responseType;

            if (rawType == Call.class) {
                responseType = getParameterUpperBound(0, returnType);
            } else if (rawType == MultiCall.class) {
                responseType = getParameterUpperBound(1, returnType);
            } else {
                return null;
            }

            return new CallAdapter<Object, Object>() {
                @Override
                public Type responseType() {
                    return responseType;
                }

                @Override
                public Call<?> adapt(Call<Object> call) {
                    return call;
                }

                @Override
                public MultiCall<?, ?> adapt(MultiCall<?, Object> call) {
                    return call;
                }
            };
        }
    }
}
