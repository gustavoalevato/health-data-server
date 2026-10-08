package br.com.zontec.watchmonitor.server.sse.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class SseConnection {

    private final String id;
    private final String userId;
    private final Set<String> channels;
    private final SseEmitter emitter;
    private final Instant connectedAt;
    private final AtomicBoolean active;

    public SseConnection(String id, String userId, Set<String> channels, SseEmitter emitter) {
        this.id = id;
        this.userId = userId;
        this.channels = Collections.unmodifiableSet(channels);
        this.emitter = emitter;
        this.connectedAt = Instant.now();
        this.active = new AtomicBoolean(true);
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public Set<String> channels() {
        return channels;
    }

    public SseEmitter emitter() {
        return emitter;
    }

    public Instant connectedAt() {
        return connectedAt;
    }

    public boolean isActive() {
        return active.get();
    }

    public boolean deactivate() {
        return active.compareAndSet(true, false);
    }
}
