package br.com.zontec.watchmonitor.server.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EcgDataPayload(
        LocalDateTime ecgTimestamp,
        BigDecimal gain,
        BigDecimal zeroVoltage,
        Integer leadType,
        Integer samplingRate,
        Integer totalPackets,
        Integer currentPacket,
        String dataPoints,
        LocalDateTime timestamp) {
}
