package br.com.zontec.watchmonitor.server.sse.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record SsePublishRequest(
        String event,
        String id,
        JsonNode data
) {
}
