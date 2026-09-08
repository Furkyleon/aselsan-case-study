package com.aselsan.queuemonitor.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.dto.request.AddWorkersRequest;
import com.aselsan.queuemonitor.dto.request.StartSimulationRequest;
import com.aselsan.queuemonitor.dto.request.StopSimulationRequest;
import com.aselsan.queuemonitor.dto.response.SimulationStatusResponse;
import com.aselsan.queuemonitor.service.MetricsService;
import com.aselsan.queuemonitor.service.SimulationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;
    private final MetricsService metricsService;

    public SimulationController(
            SimulationService simulationService,
            MetricsService metricsService
    ) {
        this.simulationService = simulationService;
        this.metricsService = metricsService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public SimulationStatusResponse start(@Valid @RequestBody StartSimulationRequest request) {
        simulationService.start(
                request.senderCount(),
                request.receiverCount(),
                request.queueCapacity()
        );

        return metricsService.refresh();
    }

    @PostMapping("/workers")
    @ResponseStatus(HttpStatus.CREATED)
    public SimulationStatusResponse addWorkers(@Valid @RequestBody AddWorkersRequest request) {
        simulationService.addWorkers(request.type(), request.count());
        return metricsService.refresh();
    }

    @DeleteMapping("/workers/{workerId}")
    public SimulationStatusResponse stopWorker(@PathVariable UUID workerId) {
        simulationService.stopWorker(workerId);
        return metricsService.refresh();
    }

    @PostMapping("/stop")
    public SimulationStatusResponse stop(@Valid @RequestBody StopSimulationRequest request) {
        switch (request.scope()) {
            case ALL -> simulationService.stopAll();
            case SENDERS -> simulationService.stopWorkers(WorkerType.SENDER);
            case RECEIVERS -> simulationService.stopWorkers(WorkerType.RECEIVER);
        }

        return metricsService.refresh();
    }

    @GetMapping("/status")
    public SimulationStatusResponse getStatus() {
        return metricsService.getStatus();
    }
}
