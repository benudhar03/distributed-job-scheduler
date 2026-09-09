package com.ar.scheduling.health;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.CompositeHealthContributor;

import java.util.Map;

@Configuration
public class HealthGroupsConfig {

    @Bean
    public HealthContributor customHealthContributor(
            MongoHealthIndicator mongoHealth,
            KafkaHealthIndicator kafkaHealth,
            SchedulingServiceHealthIndicator schedulingHealth,
            MemoryHealthIndicator memoryHealth) {

        Map<String, HealthIndicator> indicators = Map.of(
                "mongo", mongoHealth,
                "kafka", kafkaHealth,
                "scheduling-service", schedulingHealth,
                "memory", memoryHealth
        );

        return CompositeHealthContributor.fromMap(indicators);
    }
}