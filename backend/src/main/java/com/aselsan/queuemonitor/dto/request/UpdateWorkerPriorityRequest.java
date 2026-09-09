package com.aselsan.queuemonitor.dto.request;

import com.aselsan.queuemonitor.domain.WorkerType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Bir worker grubuna uygulanacak thread önceliği")
public record UpdateWorkerPriorityRequest(
        @Schema(description = "Önceliği değiştirilecek worker tipi", example = "SENDER")
        @NotNull(message = "type is required")
        WorkerType type,

        @Schema(description = "Java thread önceliği", example = "7", minimum = "1", maximum = "10")
        @Min(value = Thread.MIN_PRIORITY, message = "priority must be at least 1")
        @Max(value = Thread.MAX_PRIORITY, message = "priority must be at most 10")
        int priority
) {
}
