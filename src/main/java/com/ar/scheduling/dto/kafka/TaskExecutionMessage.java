package com.ar.scheduling.dto.kafka;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Data
public class TaskExecutionMessage {
    private String taskId;
    private String taskName;
    private String taskType;
    private String scheduleType;
    private String payload;
    private Map<String, String> metadata;
    private LocalDateTime scheduledTime;
    private Integer retryCount;
    private Integer maxRetries;
    private String priority;
    private String createdBy;
    private LocalDateTime timestamp;
}