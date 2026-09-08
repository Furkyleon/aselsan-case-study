package com.aselsan.queuemonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.worker.ManagedWorker;

class SimulationServiceTest {

    private WorkerManager workerManager;
    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        workerManager = new WorkerManager();
        simulationService = new SimulationService(workerManager);
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
    void shouldRejectAddingWorkersBeforeSimulationStarts() {
        assertThrows(
                IllegalStateException.class,
                () -> simulationService.addWorkers(WorkerType.SENDER, 1)
        );
    }

    private long countWorkers(WorkerType type) {
        return workersOfType(type).size();
    }

    private List<ManagedWorker> workersOfType(WorkerType type) {
        return simulationService.getWorkers().stream()
                .filter(worker -> worker.getType() == type)
                .toList();
    }
}
