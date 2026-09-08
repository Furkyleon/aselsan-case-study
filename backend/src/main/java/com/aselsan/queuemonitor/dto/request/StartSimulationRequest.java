package com.aselsan.queuemonitor.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record StartSimulationRequest(
        @PositiveOrZero(message = "senderCount cannot be negative")
        int senderCount,

        @PositiveOrZero(message = "receiverCount cannot be negative")
        int receiverCount,

        @Positive(message = "queueCapacity must be greater than zero")
        int queueCapacity
) {
}
