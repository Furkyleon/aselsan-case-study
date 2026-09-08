package com.aselsan.queuemonitor.dto.request;

import com.aselsan.queuemonitor.domain.WorkerType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Çalışan simülasyona eklenecek worker bilgisi")
public record AddWorkersRequest(
        @Schema(description = "Worker tipi", example = "SENDER")
        @NotNull(message = "type is required")
        WorkerType type,

        @Schema(description = "Eklenecek worker sayısı", example = "1", minimum = "1")
        @Positive(message = "count must be greater than zero")
        int count
) {
}
