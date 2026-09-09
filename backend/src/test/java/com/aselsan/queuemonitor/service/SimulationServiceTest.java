package com.aselsan.queuemonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aselsan.queuemonitor.config.SimulationProperties;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.worker.ManagedWorker;

class SimulationServiceTest {

    private static final SimulationProperties TEST_PROPERTIES =
            new SimulationProperties(
                    10,
                    100,
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(1)
            );

    private WorkerManager workerManager;
    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        workerManager = new WorkerManager();
        simulationService = new SimulationService(
                workerManager,
                TEST_PROPERTIES
        );
    }

    @AfterEach
    void tearDown() {
        workerManager.close();
    }

    @Test
    void shouldStartSimulation() {
        simulationService.start(2, 1, 5);

        int queueCapacity = simulationService.getQueue().size()
                + simulationService.getQueue().remainingCapacity();

        assertTrue(simulationService.isRunning());
        assertEquals(5, queueCapacity);
        assertEquals(2, countWorkers(WorkerType.SENDER));
        assertEquals(1, countWorkers(WorkerType.RECEIVER));
    }

    @Test
    void shouldRejectSecondStartWhileSimulationIsRunning() {
        simulationService.start(1, 0, 5);

        assertThrows(
                IllegalStateException.class,
                () -> simulationService.start(1, 1, 10)
        );
    }

    @Test
    void shouldAddWorkersToRunningSimulation() {
        simulationService.start(1, 0, 5);

        List<UUID> addedWorkerIds = simulationService.addWorkers(
                WorkerType.RECEIVER,
                2
        );

        assertEquals(2, addedWorkerIds.size());
        assertEquals(1, countWorkers(WorkerType.SENDER));
        assertEquals(2, countWorkers(WorkerType.RECEIVER));
    }

    @Test
    void shouldUpdatePrioritiesForSelectedActiveWorkerType() {
        simulationService.start(2, 1, 5);

        int updatedWorkers = simulationService.updateWorkerPriority(
                WorkerType.SENDER,
                7
        );

        assertEquals(2, updatedWorkers);
        assertTrue(workersOfType(WorkerType.SENDER).stream()
                .allMatch(worker -> worker.getPriority() == 7));
        assertTrue(workersOfType(WorkerType.RECEIVER).stream()
                .allMatch(worker -> worker.getPriority() == Thread.NORM_PRIORITY));
    }

    @Test
    void shouldRejectPriorityUpdateWhenTypeHasNoActiveWorkers() {
        simulationService.start(1, 0, 5);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> simulationService.updateWorkerPriority(
                        WorkerType.RECEIVER,
                        7
                )
        );

        assertEquals(
                "No active RECEIVER worker found",
                exception.getMessage()
        );
    }

    @Test
    void shouldStopOneWorkerByType() {
        simulationService.start(2, 1, 5);

        UUID stoppedWorkerId = simulationService.stopOneWorker(
                WorkerType.SENDER
        );

        ManagedWorker stoppedWorker = simulationService.getWorkers().stream()
                .filter(worker -> worker.getId().equals(stoppedWorkerId))
                .findFirst()
                .orElseThrow();

        assertFalse(stoppedWorker.isRunning());
        assertEquals(1, countActiveWorkers(WorkerType.SENDER));
        assertEquals(1, countActiveWorkers(WorkerType.RECEIVER));
        assertTrue(simulationService.isRunning());
    }

    @Test
    void shouldRejectStoppingOneWorkerWhenTypeHasNoActiveWorkers() {
        simulationService.start(0, 1, 5);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> simulationService.stopOneWorker(WorkerType.SENDER)
        );

        assertEquals(
                "No active SENDER worker found",
                exception.getMessage()
        );
    }

    @Test
    void shouldStopWorkersByType() {
        simulationService.start(1, 1, 5);

        simulationService.stopWorkers(WorkerType.SENDER);

        assertFalse(workersOfType(WorkerType.SENDER).stream()
                .anyMatch(ManagedWorker::isRunning));
        assertTrue(simulationService.isRunning());

        simulationService.stopWorkers(WorkerType.RECEIVER);

        assertFalse(simulationService.isRunning());
    }

    @Test
    void shouldStopAllWorkers() {
        simulationService.start(2, 2, 5);

        simulationService.stopAll();

        assertFalse(simulationService.isRunning());
        assertTrue(simulationService.getWorkers().stream()
                .noneMatch(ManagedWorker::isRunning));
    }

    @Test
    void shouldRejectStopAllBeforeSimulationStarts() {
        assertThrows(
                IllegalStateException.class,
                simulationService::stopAll
        );
    }

    @Test
    void shouldResetPreviousWorkersWhenStartingAgain() {
        simulationService.start(2, 0, 5);
        simulationService.stopAll();

        simulationService.start(0, 1, 3);

        assertEquals(0, countWorkers(WorkerType.SENDER));
        assertEquals(1, countWorkers(WorkerType.RECEIVER));
        assertEquals(1, simulationService.getWorkers().size());
    }

    @Test
    void shouldRejectInvalidStartParameters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> simulationService.start(-1, 1, 5)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> simulationService.start(1, -1, 5)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> simulationService.start(0, 0, 5)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> simulationService.start(1, 1, 0)
        );
    }

    @Test
    void shouldRejectStartWhenWorkerLimitIsExceeded() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> simulationService.start(6, 5, 10)
        );

        assertEquals(
                "Total worker count cannot exceed 10",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectStartWhenQueueCapacityLimitIsExceeded() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> simulationService.start(1, 1, 101)
        );

        assertEquals(
                "queueCapacity cannot exceed 100",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectWorkerAdditionWhenWorkerLimitIsExceeded() {
        simulationService.start(1, 1, 10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> simulationService.addWorkers(WorkerType.SENDER, 9)
        );

        assertEquals(
                "Total worker count cannot exceed 10",
                exception.getMessage()
        );
        assertEquals(2, simulationService.getWorkers().size());
    }

    @Test
    void shouldRejectAddingWorkersBeforeSimulationStarts() {
        assertThrows(
                IllegalStateException.class,
                () -> simulationService.addWorkers(WorkerType.SENDER, 1)
        );
    }

    private long countWorkers(WorkerType type) {
        return workersOfType(type).size();
    }

    private long countActiveWorkers(WorkerType type) {
        return workersOfType(type).stream()
                .filter(ManagedWorker::isRunning)
                .count();
    }

    private List<ManagedWorker> workersOfType(WorkerType type) {
        return simulationService.getWorkers().stream()
                .filter(worker -> worker.getType() == type)
                .toList();
    }
}
