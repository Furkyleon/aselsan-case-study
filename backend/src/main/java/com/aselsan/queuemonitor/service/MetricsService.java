package com.aselsan.queuemonitor.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.Message;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.dto.response.MessageFlowStatusResponse;
import com.aselsan.queuemonitor.dto.response.QueueStatusResponse;
import com.aselsan.queuemonitor.dto.response.SimulationStatusResponse;
import com.aselsan.queuemonitor.dto.response.WorkerDetailResponse;
import com.aselsan.queuemonitor.dto.response.WorkerStatusResponse;
import com.aselsan.queuemonitor.worker.ManagedWorker;

@Service
public class MetricsService {

    private final SimulationService simulationService;
    private final AtomicReference<SimulationStatusResponse> latestStatus =
            new AtomicReference<>();

    public MetricsService(SimulationService simulationService) {
        this.simulationService = Objects.requireNonNull(
                simulationService,
                "simulationService cannot be null"
        );
    }

    public SimulationStatusResponse getStatus() {
        SimulationStatusResponse status = latestStatus.get();

        return status == null ? refresh() : status;
    }

    public SimulationStatusResponse refresh() {
        SimulationStatusResponse status = createStatus();
        latestStatus.set(status);
        return status;
    }

    @Scheduled(fixedRateString = "${simulation.metrics-interval:1s}")
    public void collectMetrics() {
        refresh();
    }

    private SimulationStatusResponse createStatus() {
        Collection<ManagedWorker> workers = simulationService.getWorkers();

        return new SimulationStatusResponse(
                simulationService.isRunning(),
                createQueueStatus(),
                createMessageFlowStatus(workers),
                createWorkerStatus(workers, WorkerType.SENDER),
                createWorkerStatus(workers, WorkerType.RECEIVER),
                createWorkerDetails(workers),
                Instant.now()
        );
    }

    private MessageFlowStatusResponse createMessageFlowStatus(
            Collection<ManagedWorker> workers
    ) {
        long produced = 0;
        long consumed = 0;

        for (ManagedWorker worker : workers) {
            if (worker.getType() == WorkerType.SENDER) {
                produced += worker.getProcessedMessageCount();
            } else {
                consumed += worker.getProcessedMessageCount();
            }
        }

        return new MessageFlowStatusResponse(produced, consumed);
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
                terminated,
                simulationService.getWorkerPriority(type)
        );
    }

    private List<WorkerDetailResponse> createWorkerDetails(
            Collection<ManagedWorker> workers
    ) {
        return workers.stream()
                .sorted(Comparator
                        .comparing(ManagedWorker::getType)
                        .thenComparing(worker -> worker.getId().toString()))
                .map(worker -> new WorkerDetailResponse(
                        worker.getId(),
                        worker.getType(),
                        worker.getJvmState(),
                        worker.getActivityState(),
                        worker.getPriority(),
                        worker.isRunning(),
                        worker.getProcessedMessageCount()
                ))
                .toList();
    }

    private MetricState classify(ManagedWorker worker) {
        ActivityState activityState = worker.getActivityState();

        if (activityState == ActivityState.STOPPED
                || activityState == ActivityState.FAILED
                || (!worker.isRunning() && activityState != ActivityState.STARTING)) {
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
