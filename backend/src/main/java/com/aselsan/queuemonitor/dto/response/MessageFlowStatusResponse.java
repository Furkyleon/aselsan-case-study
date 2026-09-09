package com.aselsan.queuemonitor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simülasyon boyunca başarıyla işlenen mesaj toplamları")
public record MessageFlowStatusResponse(
        @Schema(description = "Sender worker'ların queue'ye eklediği toplam mesaj", example = "42")
        long produced,

        @Schema(description = "Receiver worker'ların queue'dan aldığı toplam mesaj", example = "38")
        long consumed
) {
}
