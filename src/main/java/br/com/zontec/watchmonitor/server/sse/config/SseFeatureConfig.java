package br.com.zontec.watchmonitor.server.sse.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SseProperties.class)
public class SseFeatureConfig {

    @Bean(name = "sseDispatchExecutor")
    public Executor sseDispatchExecutor(SseProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("sse-dispatch-");
        executor.setCorePoolSize(properties.executorCorePoolSize());
        executor.setMaxPoolSize(properties.executorMaxPoolSize());
        executor.setQueueCapacity(properties.executorQueueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
