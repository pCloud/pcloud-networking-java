package com.pcloud.networking.client;

import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;

class Utils {
    private Utils() {
    }

    static final Executor IMMEDIATE_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    static class TestExecutor implements Executor {

        private final BlockingDeque<Runnable> queue = new LinkedBlockingDeque<>();

        public Queue<Runnable> getScheduledTasks() {
            return queue;
        }

        @Override
        public void execute(Runnable command) {
            queue.add(command);
        }

        public void flush() {
            Runnable next;
            while ((next = queue.poll()) != null) {
                next.run();
            }
        }
    }
}
