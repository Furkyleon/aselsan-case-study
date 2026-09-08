package com.aselsan.queuemonitor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Yeni simülasyonun başlangıç ayarları")
public record StartSimulationRequest(
        @Schema(description = "Başlatılacak sender sayısı", example = "2", minimum = "0")
        @PositiveOrZero(message = "senderCount cannot be negative")
        int senderCount,

        @Schema(description = "Başlatılacak receiver sayısı", example = "1", minimum = "0")
        @PositiveOrZero(message = "receiverCount cannot be negative")
        int receiverCount,

        @Schema(description = "Bounded queue kapasitesi", example = "10", minimum = "1")
        @Positive(message = "queueCapacity must be greater than zero")
        int queueCapacity
) {
}
