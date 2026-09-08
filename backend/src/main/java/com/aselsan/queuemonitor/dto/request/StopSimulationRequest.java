package com.aselsan.queuemonitor.dto.request;

import com.aselsan.queuemonitor.domain.StopScope;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Durdurulacak worker grubunu belirtir")
public record StopSimulationRequest(
        @Schema(description = "Durdurma kapsamı", example = "ALL")
        @NotNull(message = "scope is required")
        StopScope scope
) {
}
