package br.com.zontec.watchmonitor.server.messaging;

import org.springframework.amqp.AmqpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.config.RabbitHealthProperties;
import br.com.zontec.watchmonitor.server.messaging.dto.DeviceHealthEvent;

@Component
public class HealthDataProducer {

    private static final Logger log = LoggerFactory.getLogger(HealthDataProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitHealthProperties properties;

    public HealthDataProducer(RabbitTemplate rabbitTemplate, RabbitHealthProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(DeviceHealthEvent event) {
        if (!properties.enabled()) {
            log.debug("RabbitMQ health producer disabled. Event skipped for IMEI {} and type {}",
                    event.imei(), event.type());
            return;
        }

        try {
            rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), event);
            log.info("Health event published to RabbitMQ for IMEI {} with type {}", event.imei(), event.type());
        } catch (AmqpException ex) {
            log.error("Failed to publish health event to RabbitMQ for IMEI {} with type {}",
                    event.imei(), event.type(), ex);
        }
    }
}
