package com.ar.scheduling.dto;

import com.ar.scheduling.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Task status response")
public record TaskStatusResponse(
    
    @Schema(description = "Task ID")
    String taskId,
    
    @Schema(description = "Current status")
    TaskStatus status,
    
    @Schema(description = "Last execution time")
    LocalDateTime lastExecutionTime,
    
    @Schema(description = "Next scheduled time")
    LocalDateTime nextScheduledTime,
    
    @Schema(description = "Retry count")
    Integer retryCount,
    
    @Schema(description = "Error message if any")
    String errorMessage,
    
    @Schema(description = "Additional status details")
    String details
) {}