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

package com.pcloud;

import com.pcloud.networking.ApiComposer;
import com.pcloud.networking.CallAdapter;
import com.pcloud.networking.Types;
import rx.Emitter;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Cancellable;
import rx.functions.Func0;
import rx.observables.SyncOnSubscribe;
import com.pcloud.networking.Call;
import com.pcloud.networking.MultiCall;
import com.pcloud.networking.MultiCallback;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Dimitard on 25.4.2017 г..
 */
public class RxCallAdapter<T> implements CallAdapter<T, Observable<T>> {
    public static final CallAdapter.Factory FACTORY = new RxCallAdapterFactory();

    private final Type responseType;

    private RxCallAdapter(Type responseType) {
        this.responseType = responseType;
    }

    @Override
    public Type responseType() {
        return responseType;
    }

    @Override
    public Observable<T> adapt(final Call<T> call) {

        return Observable.<T>create(SyncOnSubscribe.createSingleState(new Func0<Call<T>>() {
            @Override
            public Call<T> call() {
                return call.clone();
            }
        }, new Action2<Call<T>, Observer<? super T>>() {
            @Override
            public void call(Call<T> callClone, Observer<? super T> observer) {
                try {
                    observer.onNext(callClone.execute());
                    observer.onCompleted();
                } catch (Throwable throwable) {
                    observer.onError(throwable);
                }
            }
        }, new Action1<Call<T>>() {
            @Override
            public void call(Call<T> tCall) {
                tCall.cancel();
            }
        }));
    }

    @Override
    public Observable<T> adapt(final MultiCall<?, T> call) {
        return Observable.fromEmitter(new Action1<Emitter<T>>() {
            @Override
            public void call(final Emitter<T> emitter) {
                final MultiCall<?, T> multiCall = call.clone();
                MultiCallback callback = new MultiCallback<Object, T>() {
                    @Override
                    public void onFailure(MultiCall<Object, T> call1, IOException e, List<T> completedResponses) {
                        emitter.onError(e);
                    }

                    @Override
                    public void onResponse(MultiCall<Object, T> call1, int key, T response) {
                        emitter.onNext(response);
                    }

                    @Override
                    public void onComplete(MultiCall<Object, T> call1, List<T> results) {
                        emitter.onCompleted();
                    }
                };
                emitter.setCancellation(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        multiCall.cancel();
                    }
                });
                //noinspection unchecked
                multiCall.enqueue(callback);
            }
        }, Emitter.BackpressureMode.BUFFER);
    }

    static class RxCallAdapterFactory extends CallAdapter.Factory {

        @Override
        public CallAdapter<?, ?> get(ApiComposer apiComposer, Method method) {
            Type returnType = method.getGenericReturnType();
            Class<?> rawType = Types.getRawType(returnType);
            boolean isSingle = rawType == Single.class;
            if (rawType != Observable.class && !isSingle) {
                return null;
            }

            Type observableType = getParameterUpperBound(0, returnType);

            return new RxCallAdapter(observableType);
        }
    }
}
