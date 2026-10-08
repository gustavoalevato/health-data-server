package br.com.zontec.watchmonitor.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integration.inbound-location")
public record InboundLocationProperties(
        boolean enabled,
        String url,
        String secret) {
}
