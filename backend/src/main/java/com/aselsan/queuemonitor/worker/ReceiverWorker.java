package com.aselsan.queuemonitor.worker;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;

public class ReceiverWorker implements ManagedWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverWorker.class);

    private final UUID id = UUID.randomUUID();
    private final BlockingQueue<Message> queue;
    private final Duration interval;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong receivedMessageCount = new AtomicLong();

    private volatile ActivityState activityState = ActivityState.STARTING;
    private volatile Thread executionThread;
    private volatile int priority = Thread.NORM_PRIORITY;

    public ReceiverWorker(BlockingQueue<Message> queue, Duration interval) {
        this.queue = queue;
        this.interval = interval;
    }

    @Override
    public void run() {
        executionThread = Thread.currentThread();

        try {
            executionThread.setPriority(priority);

            while (running.get() && !executionThread.isInterrupted()) {
                consumeMessage();
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

    private void consumeMessage() {
        Message message = queue.poll();

        if (message == null) {
            activityState = ActivityState.QUEUE_EMPTY;
            return;
        }

        activityState = ActivityState.CONSUMING;
        receivedMessageCount.incrementAndGet();

        LOGGER.info(
                "Receiver {} consumed message {} from sender {} (sequence: {}, content: {})",
                id,
                message.id(),
                message.senderId(),
                message.sequenceNumber(),
                message.content()
        );
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
        return WorkerType.RECEIVER;
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
        return receivedMessageCount.get();
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
