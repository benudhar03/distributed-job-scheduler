package com.ar.scheduling.health;

import com.ar.scheduling.enums.TaskStatus;
import com.ar.scheduling.repository.ArScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SchedulingServiceHealthIndicator implements HealthIndicator {
    
    private final ArScheduledTaskRepository taskRepository;
    
    @Override
    public Health health() {
        try {
            Map<String, Object> details = Map.of(
                "service", "AR Scheduling Service",
                "status", "operational",
                "totalTasks", taskRepository.count(),
                "pendingTasks", taskRepository.countByStatus(TaskStatus.PENDING),
                "inProgressTasks", taskRepository.countByStatus(TaskStatus.IN_PROGRESS),
                "completedTasks", taskRepository.countByStatus(TaskStatus.COMPLETED),
                "failedTasks", taskRepository.countByStatus(TaskStatus.FAILED)
            );
            
            return Health.up().withDetails(details).build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "AR Scheduling Service")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}