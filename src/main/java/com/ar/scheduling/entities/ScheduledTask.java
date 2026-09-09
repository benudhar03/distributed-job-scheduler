package com.ar.scheduling.entities;

import com.ar.scheduling.enums.ScheduleType;
import com.ar.scheduling.enums.TaskStatus;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Document(collection = "scheduled_tasks")
@CompoundIndex(
        name = "idx_taskname_status",
        def = "{'taskName': 1, 'status': 1}"
)
@CompoundIndex(
        name = "idx_taskname_tasktype_status",
        def = "{'taskName': 1, 'taskType': 1, 'status': 1}"
)
@CompoundIndex(
        name = "idx_status_scheduled_time",
        def = "{'status': 1, 'scheduledTime': 1}"
)
public class ScheduledTask {

    @Id
    private String id;

    @Indexed(unique = true)
    private String taskId;

    @Indexed
    private String taskName;

    @Indexed
    private String taskType;

    private ScheduleType scheduleType;

    @Indexed
    private TaskStatus status;

    @Indexed
    private LocalDateTime scheduledTime;

    @Indexed
    private LocalDateTime executionTime;

    private LocalDateTime completedTime;

    private String payload;

    private Map<String, String> metadata;

    private Integer retryCount = 0;

    private Integer maxRetries = 3;

    private String errorMessage;

    private String cronExpression;

    private LocalDateTime endTime;

    private String priority;

    private String createdBy;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public ScheduledTask() {
        this.id = UUID.randomUUID().toString();
        this.retryCount = 0;
        this.maxRetries = 3;
    }
}