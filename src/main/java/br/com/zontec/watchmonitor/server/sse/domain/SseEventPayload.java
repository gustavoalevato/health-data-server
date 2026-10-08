package br.com.zontec.watchmonitor.server.sse.domain;

public record SseEventPayload(
        String event,
        String id,
        Object data
) {

    public SseEventPayload {
        if (event == null || event.isBlank()) {
            event = "message";
        }
    }
}
