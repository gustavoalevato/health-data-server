package br.com.zontec.watchmonitor.server.messaging.dto;

import java.time.LocalDateTime;

public record DeviceHealthEvent(
        String imei,
        String type,
        LocalDateTime occurredAt,
        Object payload) {
}
