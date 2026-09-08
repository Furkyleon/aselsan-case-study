package com.aselsan.queuemonitor.worker;

import java.util.Objects;
import java.util.concurrent.Future;

import com.aselsan.queuemonitor.domain.ActivityState;

public record WorkerHandle(
        ManagedWorker worker,
        Future<?> future
) {

    public WorkerHandle {
        Objects.requireNonNull(worker, "worker cannot be null");
        Objects.requireNonNull(future, "future cannot be null");
    }

    public void stop() {
        worker.stop();
        future.cancel(true);
    }

    public boolean isTerminated() {
        if (!future.isDone()) {
            return false;
        }

        Thread.State state = worker.getJvmState();

        return state == Thread.State.TERMINATED
                || (state == Thread.State.NEW
                && worker.getActivityState() == ActivityState.STOPPED);
    }
}
