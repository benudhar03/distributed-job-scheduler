package com.ar.scheduling.dto;

import com.ar.scheduling.enums.ScheduleType;
import com.ar.scheduling.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response after scheduling a task")
public record ScheduleResponse(
    
    @Schema(description = "Unique task identifier")
    String taskId,
    
    @Schema(description = "Task name")
    String taskName,
    
    @Schema(description = "Task type")
    String taskType,
    
    @Schema(description = "Schedule type")
    ScheduleType scheduleType,
    
    @Schema(description = "Current task status")
    TaskStatus status,
    
    @Schema(description = "When the task is scheduled to run")
    LocalDateTime scheduledTime,
    
    @Schema(description = "When the task was actually executed")
    LocalDateTime executionTime,
    
    @Schema(description = "When the task was completed")
    LocalDateTime completedTime,
    
    @Schema(description = "Number of retry attempts made")
    Integer retryCount,
    
    @Schema(description = "When the task was created")
    LocalDateTime createdAt
) {}