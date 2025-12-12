package com.ar.scheduling.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemoryHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        Map<String, Object> details = Map.of(
            "maxMemory", formatBytes(maxMemory),
            "usedMemory", formatBytes(usedMemory),
            "freeMemory", formatBytes(freeMemory),
            "memoryUsage", String.format("%.2f%%", memoryUsagePercent),
            "availableProcessors", runtime.availableProcessors()
        );
        
        Health.Builder status = memoryUsagePercent > 90 ? Health.down() : Health.up();
        return status.withDetails(details).build();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        else if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        else return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}