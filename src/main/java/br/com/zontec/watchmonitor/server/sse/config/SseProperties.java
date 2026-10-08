package br.com.zontec.watchmonitor.server.sse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sse")
public record SseProperties(
        Long emitterTimeoutMillis,
        Long heartbeatIntervalMillis,
        String defaultChannel,
        Integer executorCorePoolSize,
        Integer executorMaxPoolSize,
        Integer executorQueueCapacity
) {

    public SseProperties {
        if (emitterTimeoutMillis == null || emitterTimeoutMillis < 1) {
            emitterTimeoutMillis = 30L * 60L * 1000L;
        }
        if (heartbeatIntervalMillis == null || heartbeatIntervalMillis < 1000L) {
            heartbeatIntervalMillis = 15_000L;
        }
        if (defaultChannel == null || defaultChannel.isBlank()) {
            defaultChannel = "global";
        }
        if (executorCorePoolSize == null || executorCorePoolSize < 1) {
            executorCorePoolSize = 4;
        }
        if (executorMaxPoolSize == null || executorMaxPoolSize < executorCorePoolSize) {
            executorMaxPoolSize = Math.max(executorCorePoolSize, 16);
        }
        if (executorQueueCapacity == null || executorQueueCapacity < 1) {
            executorQueueCapacity = 500;
        }
    }
}
