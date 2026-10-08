package br.com.zontec.watchmonitor.server.messaging.dto;

import java.time.LocalDateTime;

public record HeartRatePayload(
        Integer heartRate,
        LocalDateTime timestamp) {
}
