package com.aselsan.queuemonitor.dto.response;

import com.aselsan.queuemonitor.domain.WorkerType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Worker grubu öncelik güncelleme sonucu")
public record WorkerPriorityResponse(
        @Schema(description = "Güncellenen worker tipi", example = "SENDER")
        WorkerType type,

        @Schema(description = "Uygulanan Java thread önceliği", example = "7")
        int priority,

        @Schema(description = "Güncellenen aktif worker sayısı", example = "2")
        int updatedWorkers
) {
}
