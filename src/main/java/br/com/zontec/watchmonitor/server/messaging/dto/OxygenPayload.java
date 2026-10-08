package br.com.zontec.watchmonitor.server.messaging.dto;

import java.time.LocalDateTime;

public record OxygenPayload(
        Integer oxygenLevel,
        LocalDateTime timestamp) {
}
