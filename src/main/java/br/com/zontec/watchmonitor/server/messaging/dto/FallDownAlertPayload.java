package br.com.zontec.watchmonitor.server.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FallDownAlertPayload(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speed,
        String direction,
        Integer batteryPercentage,
        Integer gsmStrength,
        String statusBits,
        LocalDateTime timestamp) {
}
