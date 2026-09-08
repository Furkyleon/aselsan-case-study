package com.aselsan.queuemonitor.worker;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;

class SenderWorkerTest {

    private static final Duration LONG_INTERVAL = Duration.ofSeconds(5);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);

    @Test
    void shouldProduceMessage() throws InterruptedException {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        SenderWorker worker = new SenderWorker(queue, LONG_INTERVAL);
        Thread thread = new Thread(worker, "sender-worker-test");

        try {
            thread.start();

            Message message = queue.poll(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(message);
            assertAll(
                    () -> assertEquals(worker.getId(), message.senderId()),
                    () -> assertEquals(0, message.sequenceNumber()),
                    () -> assertTrue(message.content().contains("Message #0")),
                    () -> assertEquals(WorkerType.SENDER, worker.getType()),
                    () -> assertEquals(ActivityState.PRODUCING, worker.getActivityState()),
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
    void shouldWaitWhenQueueIsFullWithoutAdvancingSequence() throws InterruptedException {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        queue.add(TestMessages.message(99));

        SenderWorker worker = new SenderWorker(queue, Duration.ofMillis(20));
        Thread thread = new Thread(worker, "sender-full-queue-test");

        try {
            thread.start();
            await(() -> worker.getActivityState() == ActivityState.QUEUE_FULL);

            queue.take();
            Message producedMessage = queue.poll(
                    TEST_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            assertNotNull(producedMessage);
            assertEquals(0, producedMessage.sequenceNumber());
        } finally {
            stopAndJoin(worker, thread);
        }
    }

    @Test
    void shouldStopBeforeThreadStarts() {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        SenderWorker worker = new SenderWorker(queue, LONG_INTERVAL);

        worker.stop();

        assertAll(
                () -> assertEquals(ActivityState.STOPPED, worker.getActivityState()),
                () -> assertEquals(Thread.State.NEW, worker.getJvmState()),
                () -> assertFalse(worker.isRunning()),
                () -> assertTrue(queue.isEmpty())
        );
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TEST_TIMEOUT.toNanos();

        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }

        assertTrue(condition.getAsBoolean(), "Condition was not met within the timeout");
    }

    private static void stopAndJoin(SenderWorker worker, Thread thread)
            throws InterruptedException {
        worker.stop();
        thread.join(TEST_TIMEOUT.toMillis());
        assertFalse(thread.isAlive(), "Sender thread did not stop in time");
    }
}
