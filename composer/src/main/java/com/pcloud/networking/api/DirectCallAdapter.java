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

package com.pcloud.networking.api;

import com.pcloud.utils.Types;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class DirectCallAdapter {
    static final CallAdapter.Factory FACTORY = new CallAdapter.Factory() {
        @Override
        public CallAdapter<?, ?> get(ApiComposer apiComposer, Method method) {
            final Class<?> returnType = Types.getRawType(method.getGenericReturnType());
            if (!(ApiResponse.class.isAssignableFrom(returnType))) {
                return null;
            }
            return new CallAdapter<Object, Object>() {
                @Override
                public Type responseType() {
                    return returnType;
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
    };

    private DirectCallAdapter() {
        throw new UnsupportedOperationException();
    }
}
