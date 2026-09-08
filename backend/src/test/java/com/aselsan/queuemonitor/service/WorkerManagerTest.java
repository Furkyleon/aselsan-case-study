package com.aselsan.queuemonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.worker.ManagedWorker;

class WorkerManagerTest {

    private static final Duration WORKER_INTERVAL = Duration.ofSeconds(5);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);

    private WorkerManager workerManager;
    private BlockingQueue<Message> queue;

    @BeforeEach
    void setUp() {
        workerManager = new WorkerManager();
        queue = new ArrayBlockingQueue<>(10);
    }

    @AfterEach
    void tearDown() {
        workerManager.close();
    }

    @Test
    void shouldStartRequestedNumberOfWorkers() throws InterruptedException {
        List<UUID> workerIds = workerManager.startWorkers(
                WorkerType.SENDER,
                2,
                queue,
                WORKER_INTERVAL
        );

        await(() -> workerManager.getWorkers().stream().allMatch(ManagedWorker::isRunning));

        assertEquals(2, workerIds.size());
        assertEquals(2, workerIds.stream().distinct().count());
        assertEquals(2, workerManager.getWorkers().size());
        assertTrue(workerManager.getWorkers().stream()
                .allMatch(worker -> worker.getType() == WorkerType.SENDER));
    }

    @Test
    void shouldStopOnlyWorkersOfRequestedType() throws InterruptedException {
        workerManager.startWorkers(WorkerType.SENDER, 1, queue, WORKER_INTERVAL);
        workerManager.startWorkers(WorkerType.RECEIVER, 1, queue, WORKER_INTERVAL);

        await(() -> workerManager.getWorkers().stream().allMatch(ManagedWorker::isRunning));

        workerManager.stopWorkers(WorkerType.SENDER);

        await(() -> workerManager.getWorkers().stream()
                .filter(worker -> worker.getType() == WorkerType.SENDER)
                .noneMatch(ManagedWorker::isRunning));

        assertTrue(workerManager.getWorkers().stream()
                .filter(worker -> worker.getType() == WorkerType.RECEIVER)
                .allMatch(ManagedWorker::isRunning));
    }

    @Test
    void shouldStopWorkerById() throws InterruptedException {
        UUID workerId = workerManager.startWorkers(
                WorkerType.SENDER,
                1,
                queue,
                WORKER_INTERVAL
        ).getFirst();

        await(() -> workerManager.getWorkers().stream().allMatch(ManagedWorker::isRunning));

        workerManager.stopWorker(workerId);

        await(() -> workerManager.getWorkers().stream().noneMatch(ManagedWorker::isRunning));
        assertFalse(workerManager.getWorkers().iterator().next().isRunning());
    }

    @Test
    void shouldClearTerminatedWorkers() throws InterruptedException {
        UUID workerId = workerManager.startWorkers(
                WorkerType.SENDER,
                1,
                queue,
                WORKER_INTERVAL
        ).getFirst();

        workerManager.stopWorker(workerId);
        await(() -> {
            workerManager.clearTerminatedWorkers();
            return workerManager.getWorkers().isEmpty();
        });

        assertTrue(workerManager.getWorkers().isEmpty());
    }

    @Test
    void shouldRejectInvalidWorkerCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> workerManager.startWorkers(
                        WorkerType.SENDER,
                        0,
                        queue,
                        WORKER_INTERVAL
                )
        );
    }

    @Test
    void shouldRejectUnknownWorkerId() {
        assertThrows(
                NoSuchElementException.class,
                () -> workerManager.stopWorker(UUID.randomUUID())
        );
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TEST_TIMEOUT.toNanos();

        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }

        assertTrue(condition.getAsBoolean(), "Condition was not met within the timeout");
    }
}
