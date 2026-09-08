package com.aselsan.queuemonitor.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Service;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.worker.ManagedWorker;

@Service
public class SimulationService {

    private static final Duration WORKER_INTERVAL = Duration.ofSeconds(1);

    private final WorkerManager workerManager;

    private BlockingQueue<Message> queue;
    private boolean running;

    public SimulationService(WorkerManager workerManager) {
        this.workerManager = Objects.requireNonNull(
                workerManager,
                "workerManager cannot be null"
        );
    }

    public synchronized void start(int senderCount, int receiverCount, int queueCapacity) {
        validateStartRequest(senderCount, receiverCount, queueCapacity);

        if (isRunning()) {
            throw new IllegalStateException("Simulation is already running");
        }

        workerManager.reset();
        BlockingQueue<Message> newQueue = new ArrayBlockingQueue<>(queueCapacity);

        try {
            if (senderCount > 0) {
                workerManager.startWorkers(
                        WorkerType.SENDER,
                        senderCount,
                        newQueue,
                        WORKER_INTERVAL
                );
            }

            if (receiverCount > 0) {
                workerManager.startWorkers(
                        WorkerType.RECEIVER,
                        receiverCount,
                        newQueue,
                        WORKER_INTERVAL
                );
            }

            queue = newQueue;
            running = true;
        } catch (RuntimeException exception) {
            workerManager.reset();
            queue = null;
            running = false;
            throw exception;
        }
    }

    public synchronized List<UUID> addWorkers(WorkerType type, int count) {
        ensureRunning();

        return workerManager.startWorkers(
                type,
                count,
                queue,
                WORKER_INTERVAL
        );
    }

    public synchronized void stopWorker(UUID workerId) {
        ensureRunning();
        workerManager.stopWorker(workerId);
        refreshRunningState();
    }

    public synchronized void stopWorkers(WorkerType type) {
        ensureRunning();
        workerManager.stopWorkers(type);
        refreshRunningState();
    }

    public synchronized void stopAll() {
        workerManager.stopAll();
        running = false;
    }

    public synchronized boolean isRunning() {
        if (running) {
            refreshRunningState();
        }

        return running;
    }

    public synchronized BlockingQueue<Message> getQueue() {
        if (queue == null) {
            throw new IllegalStateException("Simulation has not been started");
        }

        return queue;
    }

    public synchronized Optional<BlockingQueue<Message>> findQueue() {
        return Optional.ofNullable(queue);
    }

    public synchronized Collection<ManagedWorker> getWorkers() {
        return workerManager.getWorkers();
    }

    private void ensureRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("Simulation is not running");
        }
    }

    private void refreshRunningState() {
        running = workerManager.getWorkers().stream()
                .anyMatch(this::isActive);
    }

    private boolean isActive(ManagedWorker worker) {
        ActivityState state = worker.getActivityState();

        return worker.isRunning() || state == ActivityState.STARTING;
    }

    private void validateStartRequest( int senderCount, int receiverCount, int queueCapacity) {
        if (senderCount < 0) {
            throw new IllegalArgumentException("senderCount cannot be negative");
        }

        if (receiverCount < 0) {
            throw new IllegalArgumentException("receiverCount cannot be negative");
        }

        if (senderCount == 0 && receiverCount == 0) {
            throw new IllegalArgumentException("At least one worker is required");
        }

        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be greater than zero");
        }
    }
}
