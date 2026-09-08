package com.aselsan.queuemonitor.dto.response;

import java.time.Instant;

public record SimulationStatusResponse(
        boolean running,
        QueueStatusResponse queue,
        WorkerStatusResponse senders,
        WorkerStatusResponse receivers,
        Instant timestamp
) {
}
