package br.com.zontec.watchmonitor.server.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BodyTemperaturePayload(
        BigDecimal temperature,
        LocalDateTime timestamp) {
}
