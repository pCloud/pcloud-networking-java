package com.pcloud.networking.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*package*/ class Connections {
    private Connections() {
        throw new UnsupportedOperationException();
    }

    /*package*/  static final ExecutorService CLEANUP_THREAD_EXECUTOR;

    private static final long THREAD_KEEP_ALIVE = 60L;
    private static final int MAX_CLEANUP_THREADS = 3;

    static {
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, "pCloud Connections Daemon");
                result.setDaemon(true);
                return result;
            }
        };

        CLEANUP_THREAD_EXECUTOR = new ThreadPoolExecutor(0 /* corePoolSize */,
                MAX_CLEANUP_THREADS /* maximumPoolSize */,
                THREAD_KEEP_ALIVE /* keepAliveTime */,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(),
                threadFactory
        );
    }


}
