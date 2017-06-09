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

package com.pcloud.networking.api.adapters;

import com.pcloud.networking.api.ApiComposer;
import com.pcloud.networking.api.Call;
import com.pcloud.networking.api.CallAdapter;
import com.pcloud.networking.api.Interactor;
import com.pcloud.networking.api.MultiCall;
import com.pcloud.utils.Types;
import rx.Emitter;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Func0;
import rx.observables.AsyncOnSubscribe;
import rx.observables.SyncOnSubscribe;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class RxCallAdapter<T> implements CallAdapter<T, Observable<T>> {

    @SuppressWarnings("unused")
    public static final CallAdapter.Factory FACTORY = new RxCallAdapterFactory();

    private final Type responseType;

    RxCallAdapter(Type responseType) {
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
        return Observable.create(AsyncOnSubscribe.createSingleState(new Func0<Object>() {
            @Override
            public Object call() {
                try {
                    return call.clone().start();
                } catch (Throwable e) {
                    return e;
                }
            }
        }, new Action3<Object, Long, Observer<Observable<? extends T>>>() {
            @Override
            public void call(Object o, final Long requested, Observer<Observable<? extends T>> observableObserver) {
                if (o instanceof Throwable) {
                    observableObserver.onError((Throwable) o);
                    return;
                }

                @SuppressWarnings("unchecked")
                final Interactor<T> interactor = (Interactor<T>) o;

                if (!interactor.hasNextResponse()) {
                    observableObserver.onCompleted();
                    return;
                }
                observableObserver.onNext(Observable.fromEmitter(new Action1<Emitter<T>>() {
                    @Override
                    public void call(Emitter<T> emitter) {
                        try {
                            int submitted = interactor.submitRequests((int) Math.min(Integer.MAX_VALUE, requested));
                            for (int i = 0; i < submitted; i++) {
                                emitter.onNext(interactor.nextResponse());
                            }
                            emitter.onCompleted();
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }
                }, Emitter.BackpressureMode.BUFFER));
            }
        }, new Action1<Object>() {
            @Override
            public void call(Object o) {
                if (o instanceof Interactor) {
                    ((Interactor) o).close();
                }
            }
        }));
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
