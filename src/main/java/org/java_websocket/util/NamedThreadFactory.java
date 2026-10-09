package org.java_websocket.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String threadPrefix;

    private final boolean daemon;

    public NamedThreadFactory(String text) {
        this.threadPrefix = text;
        this.daemon = false;
    }

    public NamedThreadFactory(String text2, boolean daemon) {
        this.threadPrefix = text2;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = this.defaultThreadFactory.newThread(runnable);
        thread.setDaemon(this.daemon);
        thread.setName(this.threadPrefix + "-" + this.threadNumber);
        return thread;
    }
}
