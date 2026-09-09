package com.aselsan.queuemonitor.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aselsan.queuemonitor.domain.WorkerType;
import com.aselsan.queuemonitor.dto.request.AddWorkersRequest;
import com.aselsan.queuemonitor.dto.request.StartSimulationRequest;
import com.aselsan.queuemonitor.dto.request.StopSimulationRequest;
import com.aselsan.queuemonitor.dto.request.UpdateWorkerPriorityRequest;
import com.aselsan.queuemonitor.dto.response.SimulationStatusResponse;
import com.aselsan.queuemonitor.dto.response.WorkerPriorityResponse;
import com.aselsan.queuemonitor.service.MetricsService;
import com.aselsan.queuemonitor.service.SimulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/simulation")
@Tag(
        name = "Simulation",
        description = "Queue simülasyonunu ve worker yaşam döngüsünü yönetir."
)
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
    @Operation(
            summary = "Simülasyonu başlat",
            description = "Belirtilen kapasitede queue ile sender ve receiver worker'ları oluşturur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simülasyon başlatıldı"),
            @ApiResponse(responseCode = "400", description = "İstek veya sınırlar geçersiz"),
            @ApiResponse(responseCode = "409", description = "Simülasyon zaten çalışıyor")
    })
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
    @Operation(
            summary = "Worker ekle",
            description = "Çalışan simülasyona belirtilen tipte worker ekler."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Worker'lar eklendi"),
            @ApiResponse(responseCode = "400", description = "İstek veya worker sınırı geçersiz"),
            @ApiResponse(responseCode = "409", description = "Simülasyon çalışmıyor")
    })
    public SimulationStatusResponse addWorkers(@Valid @RequestBody AddWorkersRequest request) {
        simulationService.addWorkers(request.type(), request.count());
        return metricsService.refresh();
    }

    @PatchMapping("/workers/priority")
    @Operation(
            summary = "Worker önceliğini değiştir",
            description = "Seçilen tipteki tüm aktif worker thread'lerine 1-10 arasında öncelik uygular."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worker öncelikleri güncellendi"),
            @ApiResponse(responseCode = "400", description = "Worker tipi veya öncelik geçersiz"),
            @ApiResponse(responseCode = "404", description = "Bu tipte aktif worker bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Simülasyon çalışmıyor")
    })
    public WorkerPriorityResponse updateWorkerPriority(
            @Valid @RequestBody UpdateWorkerPriorityRequest request
    ) {
        int updatedWorkers = simulationService.updateWorkerPriority(
                request.type(),
                request.priority()
        );

        return new WorkerPriorityResponse(
                request.type(),
                request.priority(),
                updatedWorkers
        );
    }

    @DeleteMapping("/workers")
    @Operation(
            summary = "Tek worker azalt",
            description = "Belirtilen tipteki aktif worker'lardan birini güvenli biçimde durdurur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bir worker durduruldu"),
            @ApiResponse(responseCode = "400", description = "Worker tipi geçersiz"),
            @ApiResponse(responseCode = "404", description = "Bu tipte aktif worker bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Simülasyon çalışmıyor")
    })
    public SimulationStatusResponse stopOneWorker(@RequestParam WorkerType type) {
        simulationService.stopOneWorker(type);
        return metricsService.refresh();
    }

    @DeleteMapping("/workers/{workerId}")
    @Operation(
            summary = "Worker durdur",
            description = "Kimliği verilen worker'ı güvenli biçimde durdurur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worker durduruldu"),
            @ApiResponse(responseCode = "400", description = "Worker kimliği geçersiz"),
            @ApiResponse(responseCode = "404", description = "Worker bulunamadı"),
            @ApiResponse(responseCode = "409", description = "Simülasyon çalışmıyor")
    })
    public SimulationStatusResponse stopWorker(@PathVariable UUID workerId) {
        simulationService.stopWorker(workerId);
        return metricsService.refresh();
    }

    @PostMapping("/stop")
    @Operation(
            summary = "Worker grubunu durdur",
            description = "Tüm worker'ları veya seçilen worker grubunu durdurur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seçilen worker'lar durduruldu"),
            @ApiResponse(responseCode = "400", description = "Durdurma kapsamı geçersiz"),
            @ApiResponse(responseCode = "409", description = "Simülasyon çalışmıyor")
    })
    public SimulationStatusResponse stop(@Valid @RequestBody StopSimulationRequest request) {
        switch (request.scope()) {
            case ALL -> simulationService.stopAll();
            case SENDERS -> simulationService.stopWorkers(WorkerType.SENDER);
            case RECEIVERS -> simulationService.stopWorkers(WorkerType.RECEIVER);
        }

        return metricsService.refresh();
    }

    @GetMapping("/status")
    @Operation(
            summary = "Simülasyon durumunu getir",
            description = "Queue doluluğunu ve sender/receiver durum metriklerini döndürür."
    )
    @ApiResponse(responseCode = "200", description = "Güncel simülasyon durumu")
    public SimulationStatusResponse getStatus() {
        return metricsService.getStatus();
    }
}
