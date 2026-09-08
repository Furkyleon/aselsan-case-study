package com.aselsan.queuemonitor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bounded queue doluluk bilgileri")
public record QueueStatusResponse(
        @Schema(description = "Queue içindeki mesaj sayısı", example = "4")
        int size,

        @Schema(description = "Queue toplam kapasitesi", example = "10")
        int capacity,

        @Schema(description = "Queue doluluk yüzdesi", example = "40.0")
        double occupancyPercentage
) {
}
