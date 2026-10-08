package br.com.zontec.watchmonitor.server.messaging.dto;

import java.time.LocalDateTime;

public record HeartbeatPayload(
        Integer batteryPercentage,
        Integer gsmStrength,
        Integer numSatellites,
        Integer steps,
        String status,
        LocalDateTime timestamp) {
}
