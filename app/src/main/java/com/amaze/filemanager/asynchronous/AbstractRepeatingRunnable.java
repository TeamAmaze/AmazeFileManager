package com.amaze.filemanager.asynchronous;

import androidx.annotation.NonNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRepeatingRunnable implements Runnable {

    protected final ScheduledFuture handle;

    public AbstractRepeatingRunnable(long initialDelay, long period, @NonNull TimeUnit unit, boolean startImmediately) {
        if(!startImmediately) {
            throw new UnsupportedOperationException("RepeatingRunnables are immediately executed!");
        }

        ScheduledExecutorService threadExcecutor = Executors.newScheduledThreadPool(0);
        handle = threadExcecutor.scheduleAtFixedRate(this, initialDelay, period, unit);
    }

    public boolean isAlive() {
        return handle.isDone();
    }

    /**
     *
     * @param immediately sets if the cancellation occurt right now, or after the run() function returns
     */
    public void cancel(boolean immediately) {
        handle.cancel(immediately);
    }
}
