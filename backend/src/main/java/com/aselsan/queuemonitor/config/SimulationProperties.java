package com.aselsan.queuemonitor.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "simulation")
public record SimulationProperties(
        @Min(value = 1, message = "maxWorkers must be greater than zero")
        int maxWorkers,

        @Min(value = 1, message = "maxQueueCapacity must be greater than zero")
        int maxQueueCapacity,

        @NotNull(message = "workerInterval cannot be null")
        Duration workerInterval,

        @NotNull(message = "metricsInterval cannot be null")
        Duration metricsInterval
) {

    public SimulationProperties {
        validatePositive(workerInterval, "workerInterval");
        validatePositive(metricsInterval, "metricsInterval");
    }

    private static void validatePositive(Duration duration, String propertyName) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
    }
}
