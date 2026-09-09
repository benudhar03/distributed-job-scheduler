package com.ar.scheduling.health;

import com.ar.scheduling.repository.ArScheduledTaskRepository;
import com.ar.scheduling.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MongoHealthIndicator implements HealthIndicator {
    
    private final MongoTemplate mongoTemplate;
    private final ArScheduledTaskRepository taskRepository;
    
    @Override
    public Health health() {
        try {
            // Test basic MongoDB connection
            mongoTemplate.executeCommand("{ ping: 1 }");
            
            // Get some statistics
            long totalTasks = taskRepository.count();
            long pendingTasks = taskRepository.countByStatus(TaskStatus.PENDING);
            long failedTasks = taskRepository.countByStatus(TaskStatus.FAILED);
            
            return Health.up()
                .withDetail("database", "MongoDB")
                .withDetail("status", "connected")
                .withDetail("totalTasks", totalTasks)
                .withDetail("pendingTasks", pendingTasks)
                .withDetail("failedTasks", failedTasks)
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "MongoDB")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}