package com.aselsan.queuemonitor.worker;

import java.time.Instant;
import java.util.UUID;

import com.aselsan.queuemonitor.domain.Message;

final class TestMessages {

    private TestMessages() {
    }

    static Message message(long sequenceNumber) {
        UUID senderId = UUID.randomUUID();

        return new Message(
                UUID.randomUUID(),
                senderId,
                sequenceNumber,
                "Test message #" + sequenceNumber,
                Instant.now()
        );
    }
}
