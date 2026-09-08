package com.aselsan.queuemonitor.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simülasyonun güncel queue ve worker durumu")
public record SimulationStatusResponse(
        @Schema(description = "En az bir worker aktifse true", example = "true")
        boolean running,

        QueueStatusResponse queue,

        WorkerStatusResponse senders,

        WorkerStatusResponse receivers,

        @Schema(description = "Metriklerin oluşturulma zamanı")
        Instant timestamp
) {
}
