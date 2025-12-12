package com.ar.scheduling.dto;

import com.ar.scheduling.enums.ScheduleType;
import com.ar.scheduling.enums.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Schema(description = "Request for scheduling a new task")
public record ScheduleRequest(
    
    @NotBlank(message = "Task name is required")
    @Schema(description = "Unique name for the task", example = "Send Monthly Report")
    String taskName,
    
    @NotBlank(message = "Task type is required")
    @Schema(description = "Type of task to be executed", example = "EMAIL_NOTIFICATION")
    String taskType,
    
    @NotNull(message = "Schedule type is required")
    @Schema(description = "Type of scheduling")
    ScheduleType scheduleType,
    
    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    @Schema(description = "When the task should be executed")
    LocalDateTime scheduledTime,
    
    @Schema(description = "Task execution payload in JSON format")
    String payload,
    
    @Schema(description = "Additional metadata for the task")
    Map<String, String> metadata,
    
    @Schema(description = "Cron expression for recurring tasks", example = "0 0 9 * * MON")
    String cronExpression,
    
    @Schema(description = "End time for recurring tasks")
    LocalDateTime endTime,
    
    @Schema(description = "Maximum number of retry attempts", example = "3")
    Integer maxRetries,
    
    @Schema(description = "Priority of the task")
    TaskPriority priority,
    
    @Schema(description = "ID of the user who created the task")
    String createdBy
) {}