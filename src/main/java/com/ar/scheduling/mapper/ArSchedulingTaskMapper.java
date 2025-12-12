package com.ar.scheduling.mapper;

import com.ar.scheduling.dto.ScheduleRequest;
import com.ar.scheduling.dto.ScheduleResponse;
import com.ar.scheduling.dto.TaskStatusResponse;
import com.ar.scheduling.entities.ScheduledTask;
import com.ar.scheduling.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ArSchedulingTaskMapper {

    public ScheduledTask createTaskFromRequest(ScheduleRequest request) {
        ScheduledTask task = new ScheduledTask();
        task.setTaskId(generateTaskId());
        task.setTaskName(request.taskName());
        task.setTaskType(request.taskType());
        task.setScheduleType(request.scheduleType());
        task.setStatus(TaskStatus.PENDING);
        task.setScheduledTime(request.scheduledTime());
        task.setPayload(request.payload());
        task.setMetadata(request.metadata());
        task.setCronExpression(request.cronExpression());
        task.setEndTime(request.endTime());
        task.setMaxRetries(request.maxRetries() != null ? request.maxRetries() : 3);
        task.setPriority(request.priority() != null ? request.priority().name() : "MEDIUM");
        task.setCreatedBy(request.createdBy());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    public ScheduleResponse mapToScheduleResponse(ScheduledTask task) {
        return new ScheduleResponse(
                task.getTaskId(),
                task.getTaskName(),
                task.getTaskType(),
                task.getScheduleType(),
                task.getStatus(),
                task.getScheduledTime(),
                task.getExecutionTime(),
                task.getCompletedTime(),
                task.getRetryCount(),
                task.getCreatedAt()
        );
    }


    public TaskStatusResponse mapToTaskStatusResponse(ScheduledTask task) {
        return new TaskStatusResponse(
                task.getTaskId(),
                task.getStatus(),
                task.getExecutionTime(),
                task.getScheduledTime(),
                task.getRetryCount(),
                task.getErrorMessage(),
                generateStatusDetails(task)
        );
    }

    private String generateTaskId() {
        return "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateStatusDetails(ScheduledTask task) {
        return switch (task.getStatus()) {
            case PENDING -> "Task is waiting to be scheduled";
            case SCHEDULED -> "Task is scheduled for execution";
            case IN_PROGRESS -> "Task is currently being executed";
            case COMPLETED -> "Task completed successfully";
            case FAILED -> "Task execution failed";
            case CANCELLED -> "Task has been cancelled";
            case RETRYING -> "Task is being retried after failure";
        };
    }
}
