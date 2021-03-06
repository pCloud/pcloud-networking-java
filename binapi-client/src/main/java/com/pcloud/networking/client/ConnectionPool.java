/*
 * Copyright (c) 2020 pCloud AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pcloud.networking.client;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An implementation to provide a pool structure for connections.
 * <p>
 * {@linkplain Connection} objects are time and resource consuming to create and should be reused as much as possible.
 * This implementation keeps and provides instances of {@linkplain Connection} to be reused.
 */
@SuppressWarnings("WeakerAccess")
public class ConnectionPool {

    private static final long NANOS_TO_MILLIS_COEF = 1000000L;
    private static final int MAX_IDLE_CONN_COUNT = 5;
    private static final long MAX_KEEP_ALIVE_DURATION = 5;

    private final int maxIdleConnections;
    private final long keepAliveDurationNs;
    private final Runnable cleanupRunnable = new Runnable() {
        @SuppressWarnings("BusyWait")
        @Override
        public void run() {
            while (true) {
                long waitNanos = cleanup(System.nanoTime());
                if (waitNanos == -1L) return;
                if (waitNanos > 0L) {
                    long waitMillis = waitNanos / NANOS_TO_MILLIS_COEF;
                    waitNanos -= (waitMillis * NANOS_TO_MILLIS_COEF);
                    try {
                        Thread.sleep(waitMillis, (int) waitNanos);
                    } catch (InterruptedException ignored) {
                        // Do nothing
                    }
                }
            }
        }
    };

    private final Set<RealConnection> connections = new LinkedHashSet<>();
    private boolean cleanupRunning;

    /**
     * Create a {@linkplain ConnectionPool} with default parameters.
     * <p>
     * By default the pool will be created with 5 maximum idle connections and it will keep
     * idle connections alive for 5 minutes before disposing of them.
     */
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
    public synchronized int connectionCount() {
        return connections.size();
    }

    /**
     * Returns a recycled connection to {@code address}, or null if no such connection exists.
     */
    synchronized RealConnection get(Endpoint endpoint) {

        /*
         * Iterate the connections in reverse order
         * and take the first connection having the same endpoint.
         * */
        Iterator<RealConnection> iterator = connections.iterator();
        while (iterator.hasNext()) {
            RealConnection connection = iterator.next();
            if (connection.endpoint().equals(endpoint)) {
                iterator.remove();
                return connection;
            }
        }

        return null;
    }

    synchronized void recycle(RealConnection connection) {
        if (maxIdleConnections > 0) {
            if (!cleanupRunning) {
                cleanupRunning = true;
                Connections.CLEANUP_THREAD_EXECUTOR.execute(cleanupRunnable);
            }
            connection.setIdle(System.nanoTime());
            connections.add(connection);
        } else {
            // No idle connections allowed, close immediately.
            connection.close(false);
        }
    }

    /**
     * Close and remove all idle connections in the pool.
     */
    public void evictAll() {
        List<RealConnection> evictedConnections = new ArrayList<>();
        synchronized (this) {
            for (Iterator<RealConnection> i = connections.iterator(); i.hasNext(); ) {
                RealConnection connection = i.next();
                evictedConnections.add(connection);
                i.remove();
            }
        }

        for (RealConnection connection : evictedConnections) {
            connection.close();
        }
    }

    long cleanup(long now) {
        int idleConnectionCount;
        RealConnection longestIdleConnection = null;
        long longestIdleDurationNs = Long.MIN_VALUE;

        // Find either a connection to evict, or the time that the next eviction is due.
        synchronized (this) {
            idleConnectionCount = connections.size();
            for (RealConnection connection : connections) {
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
                return -1L;
            }
        }

        if (longestIdleConnection != null) {
            longestIdleConnection.close(true);
        }

        // Cleanup again immediately.
        return 0L;
    }
}
