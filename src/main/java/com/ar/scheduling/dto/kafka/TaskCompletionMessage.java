package com.ar.scheduling.dto.kafka;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskCompletionMessage {
    private String taskId;
    private boolean success;
    private String message;
    private LocalDateTime completionTime;
}