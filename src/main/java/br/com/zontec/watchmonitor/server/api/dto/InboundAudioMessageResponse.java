package br.com.zontec.watchmonitor.server.api.dto;

import java.time.LocalDateTime;

public record InboundAudioMessageResponse(
        Long id,
        String imei,
        String fileName,
        LocalDateTime timestamp,
        String streamUrl
) {
}
