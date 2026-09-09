package com.aselsan.queuemonitor.worker;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.WorkerType;

import java.util.UUID;

public interface ManagedWorker extends Runnable {

    UUID getId();

    WorkerType getType();

    ActivityState getActivityState();

    Thread.State getJvmState();

    int getPriority();

    void setPriority(int priority);

    boolean isRunning();

    void stop();
}
