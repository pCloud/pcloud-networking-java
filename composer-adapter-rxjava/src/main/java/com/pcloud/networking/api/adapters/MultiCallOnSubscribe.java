package com.pcloud.networking.api.adapters;

import com.pcloud.networking.api.Interactor;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;

/**
 * Created by Georgi Neykov on 13.12.2016.
 */
public class MultiCallOnSubscribe<T> implements Observable.OnSubscribe<T> {
    private Func0<Interactor<T>> generator;

    @Override
    public void call(Subscriber<? super T> subscriber) {

    }

   /* public MultiCallOnSubscribe(Func0<Interactor<T>> generator, Action) {
        this.generator = generator;
        this.cursorMapper = cursorMapper;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        generator = generatorFactory.call();
        if (generator == null) {
            throw new IllegalStateException("CursorFactory returned null.");
        }

        MultiCallProducer<T> producer = new MultiCallProducer<>(subscriber, generator, cursorMapper);
        subscriber.setProducer(producer);
        producer.exhaust();
    }*/
}
