package br.com.zontec.watchmonitor.server.messaging.dto;

public record SleepEntryPayload(
        Integer sleepType,
        Integer durationMinutes) {
}
