/*
 * Copyright (c) 2018 pCloud AG
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

package com.pcloud.networking.api.adapters;

import com.pcloud.networking.api.ApiComposer;
import com.pcloud.networking.api.Call;
import com.pcloud.networking.api.CallAdapter;
import com.pcloud.networking.api.MultiCall;
import com.pcloud.utils.Types;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * A {@linkplain CallAdapter} implementation that returns RxJava 1.x Singles
 * <p>
 * This adapter implementation allows for declaring interface methods
 * that return instances of {@linkplain Single}, both for single
 * and batched request calls.
 * <p>
 * To install, call {@linkplain com.pcloud.networking.api.ApiComposer.Builder#addAdapterFactory(Factory)}
 * with the {@linkplain #FACTORY} object.
 *
 * @param <T> the type of the response object
 */
public class RxSingleCallAdapter<T> implements CallAdapter<T, Single<T>> {

    public static final Factory FACTORY = new Factory() {
        @Override
        public CallAdapter<?, ?> get(ApiComposer apiComposer, Method method) {
            Type returnType = method.getGenericReturnType();
            Class<?> rawType = Types.getRawType(returnType);
            if (Single.class.equals(rawType)) {
                Type observableType = getParameterUpperBound(0, returnType);
                return new RxSingleCallAdapter<>(observableType);
            }
            return null;
        }
    };

    private final Type responseType;

    RxSingleCallAdapter(Type responseType) {
        this.responseType = responseType;
    }

    @Override
    public Type responseType() {
        return responseType;
    }

    @Override
    public Single<T> adapt(final Call<T> call) throws IOException {
        return Single.create(
                new Single.OnSubscribe<T>() {
                    @Override
                    public void call(SingleSubscriber<? super T> singleSubscriber) {
                        Call<T> callClone = call.clone();
                        singleSubscriber.add(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                call.cancel();
                            }
                        }));
                        try {
                            singleSubscriber.onSuccess(callClone.execute());
                        } catch (Throwable throwable) {
                            singleSubscriber.onError(throwable);
                        }
                    }
                });
    }

    @Override
    public Single<T> adapt(MultiCall<?, T> call) throws IOException {
        throw new IllegalArgumentException("Cannot convert a `" +
                call.getClass().getCanonicalName() + "` to a `" + Single.class.getCanonicalName() + "` ");
    }
}
