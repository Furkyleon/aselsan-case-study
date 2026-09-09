package com.aselsan.queuemonitor.worker;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;

class ReceiverWorkerTest {

    private static final Duration LONG_INTERVAL = Duration.ofSeconds(5);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);

    @Test
    void shouldConsumeMessage() throws InterruptedException {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        queue.add(TestMessages.message(0));

        ReceiverWorker worker = new ReceiverWorker(queue, LONG_INTERVAL);
        Thread thread = new Thread(worker, "receiver-worker-test");

        try {
            thread.start();
            await(() -> queue.isEmpty()
                    && worker.getActivityState() == ActivityState.CONSUMING);

            assertAll(
                    () -> assertTrue(queue.isEmpty()),
                    () -> assertEquals(WorkerType.RECEIVER, worker.getType()),
                    () -> assertEquals(ActivityState.CONSUMING, worker.getActivityState()),
                    () -> assertTrue(worker.isRunning())
            );
        } finally {
            stopAndJoin(worker, thread);
        }

        assertAll(
                () -> assertEquals(ActivityState.STOPPED, worker.getActivityState()),
                () -> assertEquals(Thread.State.TERMINATED, worker.getJvmState()),
                () -> assertFalse(worker.isRunning())
        );
    }

    @Test
    void shouldWaitWhenQueueIsEmpty() throws InterruptedException {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        ReceiverWorker worker = new ReceiverWorker(queue, LONG_INTERVAL);
        Thread thread = new Thread(worker, "receiver-empty-queue-test");

        try {
            thread.start();
            await(() -> worker.getActivityState() == ActivityState.QUEUE_EMPTY);

            assertTrue(queue.isEmpty());
            assertTrue(worker.isRunning());
        } finally {
            stopAndJoin(worker, thread);
        }
    }

    @Test
    void shouldStopBeforeThreadStarts() {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        ReceiverWorker worker = new ReceiverWorker(queue, LONG_INTERVAL);

        worker.stop();

        assertAll(
                () -> assertEquals(ActivityState.STOPPED, worker.getActivityState()),
                () -> assertEquals(Thread.State.NEW, worker.getJvmState()),
                () -> assertFalse(worker.isRunning()),
                () -> assertTrue(queue.isEmpty())
        );
    }

    @Test
    void shouldApplyConfiguredPriorityWhenThreadStarts() throws InterruptedException {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        ReceiverWorker worker = new ReceiverWorker(queue, LONG_INTERVAL);
        Thread thread = new Thread(worker, "receiver-priority-test");
        worker.setPriority(Thread.MIN_PRIORITY);

        try {
            thread.start();
            await(worker::isRunning);

            assertAll(
                    () -> assertEquals(Thread.MIN_PRIORITY, worker.getPriority()),
                    () -> assertEquals(Thread.MIN_PRIORITY, thread.getPriority())
            );
        } finally {
            stopAndJoin(worker, thread);
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TEST_TIMEOUT.toNanos();

        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }

        assertTrue(condition.getAsBoolean(), "Condition was not met within the timeout");
    }

    private static void stopAndJoin(ReceiverWorker worker, Thread thread)
            throws InterruptedException {
        worker.stop();
        thread.join(TEST_TIMEOUT.toMillis());
        assertFalse(thread.isAlive(), "Receiver thread did not stop in time");
    }
}
