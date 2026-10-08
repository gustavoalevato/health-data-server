package br.com.zontec.watchmonitor.server.sse.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.zontec.watchmonitor.server.sse.domain.SseConnection;

class SseConnectionRegistryTest {

    @Test
    void shouldIndexConnectionsByChannelAndUserAndCleanupSafely() {
        SseConnectionRegistry registry = new SseConnectionRegistry();
        SseConnection connection = new SseConnection("conn-1", "alice", Set.of("alerts", "devices"), new SseEmitter());

        registry.register(connection);

        assertEquals(1, registry.countConnections());
        assertEquals(2, registry.countChannels());
        assertEquals(1, registry.findByUser("alice").size());
        assertEquals(1, registry.findByChannel("alerts").size());

        assertTrue(registry.remove("conn-1"));
        assertEquals(0, registry.countConnections());
        assertEquals(0, registry.findByUser("alice").size());
        assertEquals(0, registry.findByChannel("alerts").size());
    }
}
