package com.aselsan.queuemonitor.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;

public class SenderWorker implements ManagedWorker {
    private final UUID id = UUID.randomUUID();
    private final BlockingQueue<Message> queue;
    private final Duration interval;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong sentMessageCount = new AtomicLong();

    private volatile ActivityState activityState = ActivityState.STARTING;
    private volatile Thread executionThread;
    private volatile int priority = Thread.NORM_PRIORITY;

    public SenderWorker(BlockingQueue<Message> queue, Duration interval) {
        this.queue = queue;
        this.interval = interval;
    }

    @Override
    public void run() {
        executionThread = Thread.currentThread();

        try {
            executionThread.setPriority(priority);

            while (running.get() && !executionThread.isInterrupted()) {
                produceMessage();
                Thread.sleep(interval.toMillis());
            }
        } 
        catch (InterruptedException exception) {
            executionThread.interrupt();
        } 
        catch (RuntimeException exception) {
            activityState = ActivityState.FAILED;
            return;
        } 
        finally {
            running.set(false);

            if (activityState != ActivityState.FAILED) {
                activityState = ActivityState.STOPPED;
            }
        }
    }

    private void produceMessage() {
        long currentSequence = sentMessageCount.get();

        Message message = new Message(
                UUID.randomUUID(),
                id,
                currentSequence,
                "Sender %s - Message #%d".formatted(id, currentSequence),
                Instant.now()
        );

        boolean added = queue.offer(message);

        if (added) {
            sentMessageCount.incrementAndGet();
            activityState = ActivityState.PRODUCING;
        } else {
            activityState = ActivityState.QUEUE_FULL;
        }
    }

    @Override
    public void stop() {
        running.set(false);

        Thread thread = executionThread;

        if (thread == null) {
            activityState = ActivityState.STOPPED;
            return;
        }

        thread.interrupt();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public WorkerType getType() {
        return WorkerType.SENDER;
    }

    @Override
    public ActivityState getActivityState() {
        return activityState;
    }

    @Override
    public Thread.State getJvmState() {
        Thread thread = executionThread;

        return thread == null ? Thread.State.NEW : thread.getState();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        validatePriority(priority);
        this.priority = priority;

        Thread thread = executionThread;

        if (thread != null && thread.isAlive()) {
            thread.setPriority(priority);
        }
    }

    @Override
    public long getProcessedMessageCount() {
        return sentMessageCount.get();
    }

    @Override
    public boolean isRunning() {
        Thread thread = executionThread;

        return running.get() && thread != null && thread.isAlive();
    }

    private void validatePriority(int priority) {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority must be between "
                            + Thread.MIN_PRIORITY
                            + " and "
                            + Thread.MAX_PRIORITY
            );
        }
    }
}
