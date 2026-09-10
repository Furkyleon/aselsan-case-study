package com.aselsan.queuemonitor.dto.response;

import java.util.UUID;

import com.aselsan.queuemonitor.domain.ActivityState;
import com.aselsan.queuemonitor.domain.WorkerType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tek bir worker'ın güncel çalışma bilgileri")
public record WorkerDetailResponse(
        UUID id,

        WorkerType type,

        @Schema(description = "JVM thread durumu", example = "TIMED_WAITING")
        Thread.State jvmState,

        @Schema(description = "Worker aktivite durumu", example = "PRODUCING")
        ActivityState activityState,

        @Schema(description = "Worker thread priority değeri", example = "7")
        int priority,

        @Schema(description = "Worker aktifse true", example = "true")
        boolean running,

        @Schema(description = "Worker'ın başarıyla işlediği toplam mesaj", example = "42")
        long processedMessageCount
) {
}
