package br.com.zontec.watchmonitor.server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    TopicExchange healthExchange(RabbitHealthProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    Queue healthQueue(RabbitHealthProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Binding healthBinding(Queue healthQueue, TopicExchange healthExchange, RabbitHealthProperties properties) {
        return BindingBuilder.bind(healthQueue).to(healthExchange).with(properties.routingKey());
    }
}
