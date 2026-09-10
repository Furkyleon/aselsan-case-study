package com.aselsan.queuemonitor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bir worker tipinin thread durumlarına göre dağılımı")
public record WorkerStatusResponse(
        @Schema(description = "Toplam worker sayısı", example = "3")
        int total,

        @Schema(description = "RUNNABLE durumundaki worker sayısı", example = "1")
        int runnable,

        @Schema(description = "WAITING veya TIMED_WAITING durumundaki worker sayısı", example = "2")
        int waiting,

        @Schema(description = "BLOCKED durumundaki worker sayısı", example = "0")
        int blocked,

        @Schema(description = "Sonlandırılmış worker sayısı", example = "0")
        int terminated,

        @Schema(description = "Bu worker grubu için uygulanan thread priority", example = "5")
        int priority
) {
}
