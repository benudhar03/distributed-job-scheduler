package com.ar.scheduling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Request for updating an existing task")
public record ScheduleUpdateRequest(
    
    @Schema(description = "New scheduled time for the task")
    @Future(message = "Scheduled time must be in the future")
    LocalDateTime scheduledTime,
    
    @Schema(description = "Updated task payload")
    String payload,
    
    @Schema(description = "Updated metadata")
    Map<String, String> metadata,
    
    @Schema(description = "Updated cron expression")
    String cronExpression,
    
    @Schema(description = "Updated end time")
    LocalDateTime endTime,
    
    @Schema(description = "Updated max retries")
    Integer maxRetries
) {}