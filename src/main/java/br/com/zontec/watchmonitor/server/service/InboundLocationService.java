package br.com.zontec.watchmonitor.server.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.zontec.watchmonitor.server.config.InboundLocationProperties;
import br.com.zontec.watchmonitor.server.entities.Device;

@Service
public class InboundLocationService {

    private static final Logger log = LoggerFactory.getLogger(InboundLocationService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final InboundLocationProperties properties;

    public InboundLocationService(RestTemplate restTemplate, InboundLocationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public void sendLocation(Device device, BigDecimal latitude, BigDecimal longitude, LocalDateTime timestamp) {
        if (!properties.enabled()) {
            log.debug("Integracao de localizacao desabilitada para o device {}", device.getImei());
            return;
        }

        if (properties.url() == null || properties.url().isBlank()) {
            log.warn("Integracao de localizacao habilitada, mas a URL nao foi configurada.");
            return;
        }

        if (properties.secret() == null || properties.secret().isBlank()) {
            log.warn("Integracao de localizacao habilitada, mas o secret nao foi configurado.");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Notification-Secret", properties.secret());

        InboundLocationRequest request = new InboundLocationRequest(
                latitude,
                longitude,
                TIMESTAMP_FORMATTER.format(timestamp),
                1L);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    properties.url(),
                    new HttpEntity<>(request, headers),
                    Void.class);
            log.info("Localizacao enviada para API externa. IMEI={}, status={}", device.getImei(), response.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Falha ao enviar localizacao para API externa. IMEI={}, deviceId={}, erro={}",
                    device.getImei(), device.getId(), ex.getMessage(), ex);
        }
    }

    private record InboundLocationRequest(
            BigDecimal latitude,
            BigDecimal longitude,
            String timestamp,
            Long device_id) {
    }
}
