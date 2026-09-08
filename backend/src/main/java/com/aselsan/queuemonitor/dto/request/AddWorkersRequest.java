package com.aselsan.queuemonitor.dto.request;

import com.aselsan.queuemonitor.domain.WorkerType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddWorkersRequest(
        @NotNull(message = "type is required")
        WorkerType type,

        @Positive(message = "count must be greater than zero")
        int count
) {
}
