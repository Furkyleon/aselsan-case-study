package com.aselsan.queuemonitor.domain;

import java.time.Instant;
import java.util.UUID;

public record Message(

        /**
         * Mesajın benzersiz kimliği.
         */
        UUID id,

        /**
         * Mesajı üreten Sender worker'ın kimliği.
         */
        UUID senderId,

        /**
         * İlgili Sender tarafından üretilen mesajın sıra numarası.
         */
        long sequenceNumber,

        /**
         * Mesajın taşıdığı örnek veri.
         */
        String content,

        /**
         * Mesajın oluşturulduğu zaman.
         */
        Instant createdAt
) {
}
