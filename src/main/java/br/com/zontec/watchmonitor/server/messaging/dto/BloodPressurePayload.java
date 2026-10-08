package br.com.zontec.watchmonitor.server.messaging.dto;

import java.time.LocalDateTime;

public record BloodPressurePayload(
        Integer systolic,
        Integer diastolic,
        LocalDateTime timestamp) {
}
