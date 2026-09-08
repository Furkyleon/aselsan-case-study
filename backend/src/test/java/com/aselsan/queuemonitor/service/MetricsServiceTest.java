package com.aselsan.queuemonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aselsan.queuemonitor.config.SimulationProperties;
import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.dto.response.SimulationStatusResponse;
import com.aselsan.queuemonitor.dto.response.WorkerStatusResponse;

class MetricsServiceTest {

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(2);
    private static final SimulationProperties TEST_PROPERTIES =
            new SimulationProperties(
                    10,
                    100,
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(1)
            );

    private WorkerManager workerManager;
    private SimulationService simulationService;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        workerManager = new WorkerManager();
        simulationService = new SimulationService(
                workerManager,
                TEST_PROPERTIES
        );
        metricsService = new MetricsService(simulationService);
    }

    @AfterEach
    void tearDown() {
        workerManager.close();
    }

    @Test
    void shouldReturnEmptyStatusBeforeSimulationStarts() {
        SimulationStatusResponse status = metricsService.getStatus();

        assertFalse(status.running());
        assertEquals(0, status.queue().size());
        assertEquals(0, status.queue().capacity());
        assertEquals(0.0, status.queue().occupancyPercentage());
        assertEquals(0, status.senders().total());
        assertEquals(0, status.receivers().total());
        assertNotNull(status.timestamp());
    }

    @Test
    void shouldUpdateCachedStatusWhenMetricsAreCollected() {
        SimulationStatusResponse initialStatus = metricsService.getStatus();
        simulationService.start(1, 0, 2);

        assertFalse(initialStatus.running());
        assertFalse(metricsService.getStatus().running());

        metricsService.collectMetrics();

        assertTrue(metricsService.getStatus().running());
        assertEquals(1, metricsService.getStatus().senders().total());
    }

    @Test
    void shouldCalculateQueueOccupancyAndNormalizeTimedWaiting()
            throws InterruptedException {
        simulationService.start(1, 0, 2);

        await(() -> simulationService.getQueue().size() == 1
                && simulationService.getWorkers().stream()
                .allMatch(worker -> worker.getJvmState() == Thread.State.TIMED_WAITING));

        SimulationStatusResponse status = metricsService.getStatus();

        assertTrue(status.running());
        assertEquals(1, status.queue().size());
        assertEquals(2, status.queue().capacity());
        assertEquals(50.0, status.queue().occupancyPercentage());
        assertEquals(1, status.senders().total());
        assertEquals(1, status.senders().waiting());
    }

    @Test
    void shouldCountWorkersByType() {
        simulationService.start(2, 1, 10);

        SimulationStatusResponse status = metricsService.getStatus();

        assertEquals(2, status.senders().total());
        assertEquals(1, status.receivers().total());
        assertEquals(status.senders().total(), categorizedTotal(status.senders()));
        assertEquals(status.receivers().total(), categorizedTotal(status.receivers()));
    }

    @Test
    void shouldCountStoppedWorkersAsTerminated() throws InterruptedException {
        simulationService.start(1, 0, 2);
        simulationService.stopAll();

        await(() -> simulationService.getWorkers().stream()
                .allMatch(worker -> worker.getActivityState() == ActivityState.STOPPED));

        SimulationStatusResponse status = metricsService.getStatus();

        assertFalse(status.running());
        assertEquals(1, status.senders().total());
        assertEquals(1, status.senders().terminated());
    }

    private static int categorizedTotal(WorkerStatusResponse status) {
        return status.runnable()
                + status.waiting()
                + status.blocked()
                + status.terminated();
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TEST_TIMEOUT.toNanos();

        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }

        assertTrue(condition.getAsBoolean(), "Condition was not met within the timeout");
    }
}
