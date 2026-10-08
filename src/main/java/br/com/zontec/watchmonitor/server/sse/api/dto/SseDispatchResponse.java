package br.com.zontec.watchmonitor.server.sse.api.dto;

public record SseDispatchResponse(
        String target,
        String event,
        int subscribers
) {
}
