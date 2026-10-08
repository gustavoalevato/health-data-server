package br.com.zontec.watchmonitor.server.sse.application;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.sse.domain.SseConnection;

@Component
public class SseConnectionRegistry {

    private final ConcurrentHashMap<String, SseConnection> connectionsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> connectionIdsByChannel = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> connectionIdsByUser = new ConcurrentHashMap<>();

    public void register(SseConnection connection) {
        connectionsById.put(connection.id(), connection);
        connection.channels().forEach(channel ->
                connectionIdsByChannel.computeIfAbsent(channel, ignored -> ConcurrentHashMap.newKeySet()).add(connection.id()));
        connectionIdsByUser.computeIfAbsent(connection.userId(), ignored -> ConcurrentHashMap.newKeySet()).add(connection.id());
    }

    public boolean remove(String connectionId) {
        SseConnection connection = connectionsById.remove(connectionId);
        if (connection == null) {
            return false;
        }

        connection.deactivate();
        connection.channels().forEach(channel -> removeIndexValue(connectionIdsByChannel, channel, connectionId));
        removeIndexValue(connectionIdsByUser, connection.userId(), connectionId);
        return true;
    }

    public Collection<SseConnection> findAll() {
        return Collections.unmodifiableCollection(connectionsById.values());
    }

    public Set<SseConnection> findByChannel(String channel) {
        return resolveConnections(connectionIdsByChannel.get(channel));
    }

    public Set<SseConnection> findByUser(String userId) {
        return resolveConnections(connectionIdsByUser.get(userId));
    }

    public int countConnections() {
        return connectionsById.size();
    }

    public int countChannels() {
        return connectionIdsByChannel.size();
    }

    private Set<SseConnection> resolveConnections(Set<String> connectionIds) {
        if (connectionIds == null || connectionIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<SseConnection> resolved = new LinkedHashSet<>();
        for (String connectionId : connectionIds) {
            SseConnection connection = connectionsById.get(connectionId);
            if (connection != null && connection.isActive()) {
                resolved.add(connection);
            }
        }
        return resolved;
    }

    private void removeIndexValue(ConcurrentHashMap<String, Set<String>> index, String key, String connectionId) {
        index.computeIfPresent(key, (ignored, ids) -> {
            ids.remove(connectionId);
            return ids.isEmpty() ? null : ids;
        });
    }
}
