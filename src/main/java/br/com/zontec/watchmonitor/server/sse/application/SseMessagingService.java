package br.com.zontec.watchmonitor.server.sse.application;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.zontec.watchmonitor.server.sse.config.SseProperties;
import br.com.zontec.watchmonitor.server.sse.domain.SseConnection;
import br.com.zontec.watchmonitor.server.sse.domain.SseEventPayload;

@Service
public class SseMessagingService {

    private static final Logger log = LoggerFactory.getLogger(SseMessagingService.class);

    private final SseConnectionRegistry connectionRegistry;
    private final SseProperties properties;
    private final Executor sseDispatchExecutor;

    public SseMessagingService(
            SseConnectionRegistry connectionRegistry,
            SseProperties properties,
            @Qualifier("sseDispatchExecutor") Executor sseDispatchExecutor) {
        this.connectionRegistry = connectionRegistry;
        this.properties = properties;
        this.sseDispatchExecutor = sseDispatchExecutor;
    }

    public SseEmitter subscribe(Principal principal, List<String> requestedChannels) {
        String userId = resolveUserId(principal);
        Set<String> channels = normalizeChannels(requestedChannels);

        SseEmitter emitter = new SseEmitter(properties.emitterTimeoutMillis());
        SseConnection connection = new SseConnection(UUID.randomUUID().toString(), userId, channels, emitter);
        connectionRegistry.register(connection);

        emitter.onCompletion(() -> cleanup(connection.id(), "completed"));
        emitter.onTimeout(() -> cleanup(connection.id(), "timeout"));
        emitter.onError(error -> cleanup(connection.id(), "error: " + error.getClass().getSimpleName()));

        sendAsync(connection, new SseEventPayload(
                "connected",
                connection.id(),
                Map.of(
                        "connectionId", connection.id(),
                        "userId", connection.userId(),
                        "channels", connection.channels(),
                        "heartbeatIntervalMillis", properties.heartbeatIntervalMillis(),
                        "connectedAt", connection.connectedAt().toString())));

        log.info("SSE conectado: connectionId={}, userId={}, channels={}", connection.id(), connection.userId(), connection.channels());
        return emitter;
    }

    public int broadcast(String channel, SseEventPayload payload) {
        Set<SseConnection> targets = connectionRegistry.findByChannel(requireChannel(channel));
        targets.forEach(connection -> sendAsync(connection, payload));
        return targets.size();
    }

    public int sendToUser(String userId, SseEventPayload payload) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Usuário do SSE não informado.");
        }

        Set<SseConnection> targets = connectionRegistry.findByUser(userId.trim());
        targets.forEach(connection -> sendAsync(connection, payload));
        return targets.size();
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("connections", connectionRegistry.countConnections());
        stats.put("channels", connectionRegistry.countChannels());
        stats.put("heartbeatIntervalMillis", properties.heartbeatIntervalMillis());
        return stats;
    }

    @Scheduled(fixedDelayString = "${app.sse.heartbeat-interval-millis:15000}")
    public void sendHeartbeat() {
        SseEventPayload heartbeat = new SseEventPayload(
                "heartbeat",
                null,
                Map.of("timestamp", Instant.now().toString()));
        connectionRegistry.findAll().forEach(connection -> sendAsync(connection, heartbeat));
    }

    private void sendAsync(SseConnection connection, SseEventPayload payload) {
        if (!connection.isActive()) {
            cleanup(connection.id(), "inactive");
            return;
        }

        sseDispatchExecutor.execute(() -> sendNow(connection, payload));
    }

    private void sendNow(SseConnection connection, SseEventPayload payload) {
        if (!connection.isActive()) {
            cleanup(connection.id(), "inactive");
            return;
        }

        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event().name(payload.event());
            if (payload.id() != null && !payload.id().isBlank()) {
                eventBuilder.id(payload.id());
            }
            connection.emitter().send(eventBuilder.data(payload.data()));
        } catch (IOException | IllegalStateException e) {
            log.debug("Falha ao enviar SSE para connectionId={}: {}", connection.id(), e.getMessage());
            cleanup(connection.id(), "send-failed");
        }
    }

    private void cleanup(String connectionId, String reason) {
        if (connectionRegistry.remove(connectionId)) {
            log.info("SSE removido: connectionId={}, reason={}", connectionId, reason);
        }
    }

    private Set<String> normalizeChannels(List<String> requestedChannels) {
        Set<String> channels = new LinkedHashSet<>();
        if (requestedChannels != null) {
            requestedChannels.stream()
                    .filter(channel -> channel != null && !channel.isBlank())
                    .map(String::trim)
                    .forEach(channels::add);
        }
        if (channels.isEmpty()) {
            channels.add(properties.defaultChannel());
        }
        return channels;
    }

    private String resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("Usuário autenticado não encontrado para SSE.");
        }
        return principal.getName().trim();
    }

    private String requireChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("Canal SSE não informado.");
        }
        return channel.trim();
    }
}
