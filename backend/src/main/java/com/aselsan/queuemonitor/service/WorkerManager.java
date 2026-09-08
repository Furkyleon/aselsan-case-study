package com.aselsan.queuemonitor.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.worker.ManagedWorker;
import com.aselsan.queuemonitor.worker.ReceiverWorker;
import com.aselsan.queuemonitor.worker.SenderWorker;
import com.aselsan.queuemonitor.worker.WorkerHandle;

import jakarta.annotation.PreDestroy;

@Service
public class WorkerManager implements AutoCloseable {

    private static final Duration MINIMUM_INTERVAL = Duration.ofMillis(1);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(2);

    private final ConcurrentMap<UUID, WorkerHandle> workers = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public WorkerManager() {
        this(Executors.newThreadPerTaskExecutor(
                Thread.ofPlatform()
                        .name("queue-worker-", 0)
                        .factory()
        ));
    }

    WorkerManager(ExecutorService executorService) {
        this.executorService = Objects.requireNonNull(
                executorService,
                "executorService cannot be null"
        );
    }

    public List<UUID> startWorkers(WorkerType type, int count, BlockingQueue<Message> queue, Duration interval) {
        validateStartRequest(type, count, queue, interval);

        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> startWorker(type, queue, interval))
                .toList();
    }

    public void stopWorker(UUID workerId) {
        Objects.requireNonNull(workerId, "workerId cannot be null");

        WorkerHandle handle = workers.get(workerId);

        if (handle == null) {
            throw new NoSuchElementException("Worker not found: " + workerId);
        }

        handle.stop();
    }

    public void stopWorkers(WorkerType type) {
        Objects.requireNonNull(type, "type cannot be null");

        workers.values().stream()
                .filter(handle -> handle.worker().getType() == type)
                .forEach(WorkerHandle::stop);
    }

    public void stopAll() {
        workers.values().forEach(WorkerHandle::stop);
    }

    public Collection<ManagedWorker> getWorkers() {
        return workers.values().stream()
                .map(WorkerHandle::worker)
                .toList();
    }

    public void clearTerminatedWorkers() {
        workers.entrySet().removeIf(entry -> entry.getValue().isTerminated());
    }

    public void reset() {
        stopAll();
        workers.clear();
    }

    @Override
    @PreDestroy
    public void close() {
        stopAll();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(
                    SHUTDOWN_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
            )) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private UUID startWorker(WorkerType type, BlockingQueue<Message> queue, Duration interval) {
        ManagedWorker worker = createWorker(type, queue, interval);
        FutureTask<Void> future = new FutureTask<>(worker, null);
        WorkerHandle handle = new WorkerHandle(worker, future);

        workers.put(worker.getId(), handle);

        try {
            executorService.execute(future);
        } catch (RuntimeException exception) {
            workers.remove(worker.getId(), handle);
            worker.stop();
            throw exception;
        }

        return worker.getId();
    }

    private ManagedWorker createWorker(WorkerType type, BlockingQueue<Message> queue, Duration interval) {
        return switch (type) {
            case SENDER -> new SenderWorker(queue, interval);
            case RECEIVER -> new ReceiverWorker(queue, interval);
        };
    }

    private void validateStartRequest(WorkerType type, int count, BlockingQueue<Message> queue, Duration interval) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(queue, "queue cannot be null");
        Objects.requireNonNull(interval, "interval cannot be null");

        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than zero");
        }

        if (interval.compareTo(MINIMUM_INTERVAL) < 0) {
            throw new IllegalArgumentException("interval must be at least 1 millisecond");
        }
    }
}
