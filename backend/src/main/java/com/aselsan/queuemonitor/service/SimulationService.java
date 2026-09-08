package com.aselsan.queuemonitor.service;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Service;

import com.aselsan.queuemonitor.config.SimulationProperties;
import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.worker.ManagedWorker;

@Service
public class SimulationService {

    private final WorkerManager workerManager;
    private final SimulationProperties properties;

    private BlockingQueue<Message> queue;
    private boolean running;

    public SimulationService(
            WorkerManager workerManager,
            SimulationProperties properties
    ) {
        this.workerManager = Objects.requireNonNull(
                workerManager,
                "workerManager cannot be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties cannot be null"
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
                        properties.workerInterval()
                );
            }

            if (receiverCount > 0) {
                workerManager.startWorkers(
                        WorkerType.RECEIVER,
                        receiverCount,
                        newQueue,
                        properties.workerInterval()
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
        validateWorkerAddition(type, count);

        return workerManager.startWorkers(
                type,
                count,
                queue,
                properties.workerInterval()
        );
    }

    public synchronized void stopWorker(UUID workerId) {
        ensureRunning();
        workerManager.stopWorker(workerId);
        refreshRunningState();
    }

    public synchronized UUID stopOneWorker(WorkerType type) {
        ensureRunning();

        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        ManagedWorker worker = workerManager.getWorkers().stream()
                .filter(this::isActive)
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No active " + type + " worker found"
                ));

        workerManager.stopWorker(worker.getId());
        refreshRunningState();
        return worker.getId();
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

    private void validateStartRequest(int senderCount, int receiverCount, int queueCapacity) {
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

        long totalWorkers = (long) senderCount + receiverCount;
        if (totalWorkers > properties.maxWorkers()) {
            throw new IllegalArgumentException(
                    "Total worker count cannot exceed " + properties.maxWorkers()
            );
        }

        if (queueCapacity > properties.maxQueueCapacity()) {
            throw new IllegalArgumentException(
                    "queueCapacity cannot exceed " + properties.maxQueueCapacity()
            );
        }
    }

    private void validateWorkerAddition(WorkerType type, int count) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than zero");
        }

        long activeWorkerCount = workerManager.getWorkers().stream()
                .filter(this::isActive)
                .count();

        if (activeWorkerCount + count > properties.maxWorkers()) {
            throw new IllegalArgumentException(
                    "Total worker count cannot exceed " + properties.maxWorkers()
            );
        }
    }
}
