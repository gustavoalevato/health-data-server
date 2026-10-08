package br.com.zontec.watchmonitor.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String username,
        String password,
        String jwtSecret,
        Long accessTokenExpirationMinutes
) {

    public SecurityProperties {
        if (username == null || username.isBlank()) {
            username = "admin";
        }
        if (password == null || password.isBlank()) {
            password = "admin";
        }
        if (jwtSecret == null || jwtSecret.isBlank()) {
            jwtSecret = "change-me-thalamus-watch-monitor-secret";
        }
        if (accessTokenExpirationMinutes == null || accessTokenExpirationMinutes < 1) {
            accessTokenExpirationMinutes = 60L;
        }
    }
}
