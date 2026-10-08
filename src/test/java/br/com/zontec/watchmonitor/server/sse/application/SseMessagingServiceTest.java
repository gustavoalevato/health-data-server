package br.com.zontec.watchmonitor.server.sse.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.zontec.watchmonitor.server.sse.config.SseProperties;
import br.com.zontec.watchmonitor.server.sse.domain.SseEventPayload;

class SseMessagingServiceTest {

    @Test
    void shouldSubscribeWithDefaultChannelAndExposeStats() {
        SseConnectionRegistry registry = new SseConnectionRegistry();
        SseProperties properties = new SseProperties(60_000L, 15_000L, "global", 1, 1, 10);
        Executor directExecutor = Runnable::run;
        SseMessagingService service = new SseMessagingService(registry, properties, directExecutor);

        SseEmitter emitter = service.subscribe(new NamedPrincipal("alice"), null);

        assertEquals(1, registry.countConnections());
        assertEquals(1, registry.findByChannel("global").size());
        assertEquals(1, service.broadcast("global", new SseEventPayload("ping", "evt-1", Map.of("ok", true))));
        assertEquals(1, service.sendToUser("alice", new SseEventPayload("private", "evt-2", Map.of("ok", true))));
        assertEquals(1, service.stats().get("connections"));

        emitter.complete();
    }

    @Test
    void shouldSubscribeToMultipleChannels() {
        SseConnectionRegistry registry = new SseConnectionRegistry();
        SseProperties properties = new SseProperties(60_000L, 15_000L, "global", 1, 1, 10);
        SseMessagingService service = new SseMessagingService(registry, properties, Runnable::run);

        service.subscribe(new NamedPrincipal("bob"), List.of("alerts", "devices"));

        assertEquals(1, registry.findByChannel("alerts").size());
        assertEquals(1, registry.findByChannel("devices").size());
        assertEquals(0, registry.findByChannel("global").size());
    }

    private record NamedPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
