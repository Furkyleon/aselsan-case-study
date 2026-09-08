package com.aselsan.queuemonitor.dto.response;

public record QueueStatusResponse(
        int size,
        int capacity,
        double occupancyPercentage
) {
}
