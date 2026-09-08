package com.aselsan.queuemonitor.dto.request;

import com.aselsan.queuemonitor.domain.StopScope;

import jakarta.validation.constraints.NotNull;

public record StopSimulationRequest(
        @NotNull(message = "scope is required")
        StopScope scope
) {
}
