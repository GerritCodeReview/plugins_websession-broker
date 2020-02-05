package com.googlesource.gerrit.plugins.websession.broker;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.concurrent.ScheduledExecutorService;

@Singleton
class CacheExecutorProvider implements Provider <ScheduledExecutorService>, LifecycleListener {
    static public String CACHE_WEBSESSIONS_RELOAD_THREAD = "Cache-Websessions-Reload";
    private ScheduledExecutorService executor;

    @Inject
    CacheExecutorProvider(WorkQueue workQueue) {
        executor = workQueue.createQueue(1, CACHE_WEBSESSIONS_RELOAD_THREAD);
    }

    @Override
    public void start() {
        // do nothing
    }

    @Override
    public void stop() {
        executor.shutdown();
        executor = null;
    }

    @Override
    public ScheduledExecutorService get() {
        return executor;
    }
}
