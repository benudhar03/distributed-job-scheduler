package com.ar.scheduling.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Health health() {
        try {
            // Test Kafka connectivity by sending a test message
            var future = kafkaTemplate.send("health-check-topic", "health-check");
            future.get(5, java.util.concurrent.TimeUnit.SECONDS); // 5 second timeout
            
            return Health.up()
                .withDetail("broker", "Kafka")
                .withDetail("status", "connected")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("broker", "Kafka")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}