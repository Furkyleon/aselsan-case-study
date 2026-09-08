package com.aselsan.queuemonitor.dto.response;

public record WorkerStatusResponse(
        int total,
        int runnable,
        int waiting,
        int blocked,
        int terminated
) {
}
