package com.aselsan.queuemonitor.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import com.aselsan.queuemonitor.config.SimulationProperties;
import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.exception.GlobalExceptionHandler;
import com.aselsan.queuemonitor.service.MetricsService;
import com.aselsan.queuemonitor.service.SimulationService;
import com.aselsan.queuemonitor.service.WorkerManager;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class SimulationControllerTest {

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
    private ValidatorFactory validatorFactory;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        workerManager = new WorkerManager();
        simulationService = new SimulationService(
                workerManager,
                TEST_PROPERTIES
        );
        metricsService = new MetricsService(simulationService);
        validatorFactory = Validation.buildDefaultValidatorFactory();

        SimulationController controller = new SimulationController(
                simulationService,
                metricsService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new SpringValidatorAdapter(validatorFactory.getValidator()))
                .build();
    }

    @AfterEach
    void tearDown() {
        workerManager.close();
        validatorFactory.close();
    }

    @Test
    void shouldStartSimulation() throws Exception {
        mockMvc.perform(post("/api/simulation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderCount": 2,
                                  "receiverCount": 1,
                                  "queueCapacity": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.queue.capacity").value(10))
                .andExpect(jsonPath("$.senders.total").value(2))
                .andExpect(jsonPath("$.receivers.total").value(1));
    }

    @Test
    void shouldRejectInvalidStartRequest() throws Exception {
        mockMvc.perform(post("/api/simulation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderCount": -1,
                                  "receiverCount": 1,
                                  "queueCapacity": 0
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.senderCount[0]")
                        .value("senderCount cannot be negative"))
                .andExpect(jsonPath("$.errors.queueCapacity[0]")
                        .value("queueCapacity must be greater than zero"));

        assertFalse(simulationService.isRunning());
    }

    @Test
    void shouldAddWorkers() throws Exception {
        simulationService.start(1, 0, 10);

        mockMvc.perform(post("/api/simulation/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "RECEIVER",
                                  "count": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivers.total").value(2));
    }

    @Test
    void shouldStopOneWorkerByType() throws Exception {
        simulationService.start(2, 1, 10);

        mockMvc.perform(delete("/api/simulation/workers")
                        .queryParam("type", "SENDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.senders.total").value(2))
                .andExpect(jsonPath("$.senders.terminated").value(1))
                .andExpect(jsonPath("$.receivers.terminated").value(0));
    }

    @Test
    void shouldRejectMissingWorkerTypeWhenStoppingOneWorker() throws Exception {
        mockMvc.perform(delete("/api/simulation/workers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void shouldStopWorkerById() throws Exception {
        simulationService.start(1, 0, 10);
        UUID workerId = simulationService.getWorkers().iterator().next().getId();

        mockMvc.perform(delete("/api/simulation/workers/{workerId}", workerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.senders.terminated").value(1));

        assertFalse(simulationService.isRunning());
    }

    @Test
    void shouldStopSenderWorkers() throws Exception {
        simulationService.start(1, 1, 10);

        mockMvc.perform(post("/api/simulation/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "SENDERS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.senders.terminated").value(1));

        assertTrue(simulationService.getWorkers().stream()
                .filter(worker -> worker.getType() == WorkerType.RECEIVER)
                .allMatch(worker -> worker.isRunning()
                        || worker.getJvmState() == Thread.State.NEW));
    }

    @Test
    void shouldStopAllWorkers() throws Exception {
        simulationService.start(1, 1, 10);

        mockMvc.perform(post("/api/simulation/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "ALL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false));

        assertFalse(simulationService.isRunning());
    }

    @Test
    void shouldReturnLatestStatus() throws Exception {
        simulationService.start(1, 0, 10);
        metricsService.refresh();

        mockMvc.perform(get("/api/simulation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.senders.total").value(1))
                .andExpect(jsonPath("$.receivers.total").value(0));
    }

    @Test
    void shouldReturnConflictWhenSimulationIsAlreadyRunning() throws Exception {
        simulationService.start(1, 0, 10);

        mockMvc.perform(post("/api/simulation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderCount": 1,
                                  "receiverCount": 1,
                                  "queueCapacity": 10
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Simulation state conflict"))
                .andExpect(jsonPath("$.detail").value("Simulation is already running"));
    }

    @Test
    void shouldReturnConflictWhenAddingWorkerBeforeSimulationStarts() throws Exception {
        mockMvc.perform(post("/api/simulation/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SENDER",
                                  "count": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Simulation is not running"));
    }

    @Test
    void shouldReturnNotFoundForUnknownWorker() throws Exception {
        simulationService.start(1, 0, 10);

        mockMvc.perform(delete(
                        "/api/simulation/workers/{workerId}",
                        UUID.randomUUID()
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void shouldReturnBadRequestForMalformedWorkerId() throws Exception {
        mockMvc.perform(delete("/api/simulation/workers/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void shouldReturnBadRequestWhenNoWorkersAreRequested() throws Exception {
        mockMvc.perform(post("/api/simulation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderCount": 0,
                                  "receiverCount": 0,
                                  "queueCapacity": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid argument"))
                .andExpect(jsonPath("$.detail").value("At least one worker is required"));
    }
}
