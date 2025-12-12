package com.ar.scheduling.health;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.health.HealthIndicator;
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