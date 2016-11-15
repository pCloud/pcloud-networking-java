package com.pcloud;

import java.util.*;
import java.util.concurrent.*;

import static com.pcloud.internal.IOUtils.closeQuietly;

/**
 * Created by Georgi Neykov on 8.11.2016.
 */

public class ConnectionPool {

    static {
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, "PCloudAPI ConnectionPool");
                result.setDaemon(true);
                return result;
            }
        };

        CLEANUP_THREAD_EXECUTOR = new ThreadPoolExecutor(0 /* corePoolSize */,
                Integer.MAX_VALUE /* maximumPoolSize */, 60L /* keepAliveTime */, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), threadFactory);
    }

    private static final Executor CLEANUP_THREAD_EXECUTOR;

    private final int maxIdleConnections;
    private final long keepAliveDurationNs;
    private final Runnable cleanupRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                long waitNanos = cleanup(System.nanoTime());
                if (waitNanos == -1) return;
                if (waitNanos > 0) {
                    long waitMillis = waitNanos / 1000000L;
                    waitNanos -= (waitMillis * 1000000L);
                    synchronized (ConnectionPool.this) {
                        try {
                            ConnectionPool.this.wait(waitMillis, (int) waitNanos);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        }
    };

    private final Deque<Connection> connections = new ArrayDeque<Connection>();
    private boolean cleanupRunning;

    public ConnectionPool() {
        this(5, 5, TimeUnit.MINUTES);
    }

    public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);

        // Put a floor on the keep alive duration, otherwise cleanup will spin loop.
        if (keepAliveDuration <= 0) {
            throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
        }
    }

    public synchronized int connectionCount() {
        return connections.size();
    }

    /**
     * Returns a recycled connection to {@code address}, or null if no such connection exists.
     */
    synchronized Connection get() {
        return connections.pollLast();
    }

    synchronized void recycle(Connection connection) {
        if (!cleanupRunning) {
            cleanupRunning = true;
            CLEANUP_THREAD_EXECUTOR.execute(cleanupRunnable);
        }
        connection.setIdle(System.nanoTime());
        connections.addFirst(connection);
    }

    /**
     * Close and remove all idle connections in the pool.
     */
    public void evictAll() {
        List<Connection> evictedConnections = new ArrayList<Connection>();
        synchronized (this) {
            for (Iterator<Connection> i = connections.iterator(); i.hasNext(); ) {
                Connection connection = i.next();
                evictedConnections.add(connection);
                i.remove();
            }
        }

        for (Connection connection : evictedConnections) {
            closeQuietly(connection);
        }
    }

    long cleanup(long now) {
        int idleConnectionCount;
        Connection longestIdleConnection = null;
        long longestIdleDurationNs = Long.MIN_VALUE;

        // Find either a connection to evict, or the time that the next eviction is due.
        synchronized (this) {
            idleConnectionCount = connections.size();
            for (Iterator<Connection> i = connections.iterator(); i.hasNext(); ) {
                Connection connection = i.next();


                // If the connection is ready to be evicted, we're done.
                long idleDurationNs = now - connection.idleAtNanos();
                if (idleDurationNs > longestIdleDurationNs) {
                    longestIdleDurationNs = idleDurationNs;
                    longestIdleConnection = connection;
                }
            }

            if (longestIdleDurationNs >= this.keepAliveDurationNs
                    || idleConnectionCount > this.maxIdleConnections) {
                // We've found a connection to evict. Remove it from the list, then close it below (outside
                // of the synchronized block).
                connections.remove(longestIdleConnection);
            } else if (idleConnectionCount > 0) {
                // A connection will be ready to evict soon.
                return keepAliveDurationNs - longestIdleDurationNs;
            } else {
                // No connections, idle or in use.
                cleanupRunning = false;
                return -1;
            }
        }

        closeQuietly(longestIdleConnection);

        // Cleanup again immediately.
        return 0;
    }
}
