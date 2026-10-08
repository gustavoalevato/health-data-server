package br.com.zontec.watchmonitor.server.messaging.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record SleepSessionPayload(
        LocalDate sleepDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer totalDurationMinutes,
        List<SleepEntryPayload> entries,
        LocalDateTime timestamp) {
}
