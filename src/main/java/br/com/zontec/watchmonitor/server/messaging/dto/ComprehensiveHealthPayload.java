package br.com.zontec.watchmonitor.server.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComprehensiveHealthPayload(
        Integer heartRate,
        Integer systolicBp,
        Integer diastolicBp,
        Integer spo2,
        BigDecimal bloodSugar,
        BigDecimal bodyTemperature,
        LocalDateTime timestamp) {
}
