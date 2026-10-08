package br.com.zontec.watchmonitor.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq.health")
public record RabbitHealthProperties(
        boolean enabled,
        String exchange,
        String queue,
        String routingKey) {
}
