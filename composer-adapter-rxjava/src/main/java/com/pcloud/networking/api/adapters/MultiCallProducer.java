package com.pcloud.networking.api.adapters;

import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func3;
import rx.internal.operators.BackpressureUtils;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.BooleanSubscription;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Georgi Neykov on 13.12.2016.
 */
public class MultiCallProducer<S, T> implements Producer {

    private Subscriber<? super T> subscriber;
    private AtomicLong requests = new AtomicLong(0);
    private Subscription subscription;
    private Semaphore drainSemaphore;
    private S generator;
    private Func3<S, Long, Observer<? super T>, Long> handler;

    public MultiCallProducer(Subscriber<? super T> subscriber,
                             S generator,
                             Func3<S, Long, Observer<? super T>, Long> handler) {
        this.subscriber = new SerializedSubscriber<>(subscriber, true);
        this.subscription = BooleanSubscription.create(new Action0() {
            @Override
            public void call() {
                // Release a permit to allow the exhaust() method to complete.
                drainSemaphore.release();
            }
        });
        this.drainSemaphore = new Semaphore(0);
        this.generator = generator;
        subscriber.add(subscription);
    }

    @Override
    public void request(long n) {
        if (n > 0L) {
            BackpressureUtils.getAndAddRequest(requests, n);
            drainSemaphore.release();
        }
    }

    /**
     * Start emitting rows upon request.
     * <p>
     *  <b>NOTE:</b> The method will not return until
     */
    public void exhaust() {
        try {
            while (!subscription.isUnsubscribed()) {
                final long requested = requests.get();
                if (requested > 0) {
                    long emmited = handler.call(generator, requested, subscriber);
                    BackpressureUtils.produced(requests, emmited);
                }
                drainSemaphore.acquire();
            }
        } catch (Throwable e) {
            subscriber.onError(e);
        }
    }
}
