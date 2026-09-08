package com.aselsan.queuemonitor.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Service;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.dto.response.QueueStatusResponse;
import com.aselsan.queuemonitor.dto.response.SimulationStatusResponse;
import com.aselsan.queuemonitor.dto.response.WorkerStatusResponse;
import com.aselsan.queuemonitor.worker.ManagedWorker;

@Service
public class MetricsService {

    private final SimulationService simulationService;

    public MetricsService(SimulationService simulationService) {
        this.simulationService = Objects.requireNonNull(
                simulationService,
                "simulationService cannot be null"
        );
    }

    public SimulationStatusResponse getStatus() {
        Collection<ManagedWorker> workers = simulationService.getWorkers();

        return new SimulationStatusResponse(
                simulationService.isRunning(),
                createQueueStatus(),
                createWorkerStatus(workers, WorkerType.SENDER),
                createWorkerStatus(workers, WorkerType.RECEIVER),
                Instant.now()
        );
    }

    private QueueStatusResponse createQueueStatus() {
        return simulationService.findQueue()
                .map(this::createQueueStatus)
                .orElseGet(() -> new QueueStatusResponse(0, 0, 0.0));
    }

    private QueueStatusResponse createQueueStatus(BlockingQueue<Message> queue) {
        int size = queue.size();
        int capacity = size + queue.remainingCapacity();
        double occupancyPercentage = capacity == 0
                ? 0.0
                : size * 100.0 / capacity;

        return new QueueStatusResponse(size, capacity, occupancyPercentage);
    }

    private WorkerStatusResponse createWorkerStatus(
            Collection<ManagedWorker> workers,
            WorkerType type
    ) {
        int runnable = 0;
        int waiting = 0;
        int blocked = 0;
        int terminated = 0;

        for (ManagedWorker worker : workers) {
            if (worker.getType() != type) {
                continue;
            }

            switch (classify(worker)) {
                case RUNNABLE -> runnable++;
                case WAITING -> waiting++;
                case BLOCKED -> blocked++;
                case TERMINATED -> terminated++;
            }
        }

        return new WorkerStatusResponse(
                runnable + waiting + blocked + terminated,
                runnable,
                waiting,
                blocked,
                terminated
        );
    }

    private MetricState classify(ManagedWorker worker) {
        ActivityState activityState = worker.getActivityState();

        if (activityState == ActivityState.STOPPED
                || activityState == ActivityState.FAILED) {
            return MetricState.TERMINATED;
        }

        return switch (worker.getJvmState()) {
            case RUNNABLE -> MetricState.RUNNABLE;
            case BLOCKED -> MetricState.BLOCKED;
            case TERMINATED -> MetricState.TERMINATED;
            case NEW, WAITING, TIMED_WAITING -> MetricState.WAITING;
        };
    }

    private enum MetricState {
        RUNNABLE,
        WAITING,
        BLOCKED,
        TERMINATED
    }
}
