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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.pcloud.IOUtils.closeQuietly;

/**
 * An implementation to provide a pool structure for connections.
 * <p>
 * {@linkplain Connection} objects are time and resource consuming to create and should be reused as much as possible.
 * This implementation keeps and provides instances of {@linkplain Connection} to be reused.
 */
@SuppressWarnings("WeakerAccess")
public class ConnectionPool {

    private static final long DEFAULT_KEEP_ALIVE_TIME_MS = 60;
    private static final long NANOS_TO_MILLIS_COEF = 1000000L;
    private static final int MAX_IDLE_CONN_COUNT = 5;
    private static final long MAX_KEEP_ALIVE_DURATION = 5;

    static {
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, "PCloudAPI ConnectionPool");
                result.setDaemon(true);
                return result;
            }
        };

        CLEANUP_THREAD_EXECUTOR =
                new ThreadPoolExecutor(0 /* corePoolSize */,
                                              Integer.MAX_VALUE /* maximumPoolSize */,
                                              DEFAULT_KEEP_ALIVE_TIME_MS /* keepAliveTime */,
                                              TimeUnit.SECONDS,
                                              new SynchronousQueue<Runnable>(),
                                              threadFactory);
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
                    long waitMillis = waitNanos / NANOS_TO_MILLIS_COEF;
                    waitNanos -= (waitMillis * NANOS_TO_MILLIS_COEF);
                    synchronized (ConnectionPool.this) {
                        try {
                            ConnectionPool.this.wait(waitMillis, (int) waitNanos);
                        } catch (InterruptedException ignored) {
                            ignored.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    private final LinkedList<Connection> connections = new LinkedList<>();
    private boolean cleanupRunning;

    /**
     * Create a {@linkplain ConnectionPool} with default parameters.
     * <p>
     * By default the pool will be created with 5 maximum idle connections and it will keep
     * idle connections alive for 5 minutes before disposing of them.
     */
    @SuppressWarnings("unused")
    public ConnectionPool() {
        this(MAX_IDLE_CONN_COUNT, MAX_KEEP_ALIVE_DURATION, TimeUnit.MINUTES);
    }

    /**
     * Create an instance of {@linkplain ConnectionPool} with your own parameters
     *
     * @param maxIdleConnections The maximum number of idle connections the pool should keep.
     *                           When the pool has this number of connections any more idle connections will be discarded.
     * @param keepAliveDuration  The amount of time the pool should keep the connections alive if they are idling.
     * @param timeUnit           The unit of time in which you provided the time duration parameter.
     * @throws IllegalArgumentException on less than 0 for number arguments and on null for the {@linkplain TimeUnit} argument
     */
    public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        if (maxIdleConnections < 0) {
            throw new IllegalArgumentException("maxIdleConnections < 0: " + maxIdleConnections);
        }

        if (keepAliveDuration <= 0) {
            // Put a floor on the keep alive duration, otherwise cleanup will spin loop.
            throw new IllegalArgumentException("keepAliveDuration < 0:" + keepAliveDuration);
        }

        if (timeUnit == null) {
            throw new IllegalArgumentException("time unit is null.");
        }

        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);
    }

    /**
     * Returns the maximum amount of idle connections the pool can keep
     *
     * @return The maximum amount of idle connections the pool can keep
     */
    public int maxIdleConnections() {
        return maxIdleConnections;
    }

    /**
     * Returns the number of connections in the pool
     *
     * @return The number of connections in the pool
     */
    @SuppressWarnings("unused")
    public synchronized int connectionCount() {
        return connections.size();
    }

    /**
     * Returns a recycled connection to {@code address}, or null if no such connection exists.
     */
    synchronized Connection get(Endpoint endpoint) {

        /*
        * Iterate the connections in reverse order
        * and take the first connection having the same endpoint.
        * */
        ListIterator<Connection> iterator = connections.listIterator(connections.size());
        while (iterator.hasPrevious()) {
            Connection connection = iterator.previous();
            if (connection.endpoint().equals(endpoint)) {
                iterator.remove();
                return connection;
            }
        }

        return null;
    }

    synchronized void recycle(Connection connection) {
        if (!cleanupRunning) {
            cleanupRunning = true;
            CLEANUP_THREAD_EXECUTOR.execute(cleanupRunnable);
        }
        ((RealConnection) connection).setIdle(System.nanoTime());
        connections.addFirst(connection);
    }

    /**
     * Close and remove all idle connections in the pool.
     */
    public void evictAll() {
        List<Connection> evictedConnections = new ArrayList<>();
        synchronized (this) {
            for (Iterator<Connection> i = connections.iterator(); i.hasNext();) {
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
            for (Iterator<Connection> i = connections.iterator(); i.hasNext();) {
                RealConnection connection = (RealConnection) i.next();

                // If the connection is ready to be evicted, we're done.
                long idleDurationNs = now - connection.idleAtNanos();
                if (idleDurationNs > longestIdleDurationNs) {
                    longestIdleDurationNs = idleDurationNs;
                    longestIdleConnection = connection;
                }
            }

            if (longestIdleDurationNs >= this.keepAliveDurationNs ||
                        idleConnectionCount > this.maxIdleConnections) {
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
