package com.ar.scheduling.util;

import com.ar.scheduling.dto.ScheduleRequest;
import com.ar.scheduling.dto.ScheduleUpdateRequest;
import com.ar.scheduling.entities.ScheduledTask;
import com.ar.scheduling.enums.ScheduleType;
import com.ar.scheduling.enums.TaskStatus;
import com.ar.scheduling.exception.TaskValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ArSchedulingUtil {

    /**
     * Validates a new schedule request.
     * @param request schedule request
     */
    public void validateScheduleRequest(ScheduleRequest request) {

        if (request.scheduledTime().isBefore(LocalDateTime.now())) {
            throw new TaskValidationException(
                    "Scheduled time must be in the future"
            );
        }

        if (request.scheduleType() == ScheduleType.CRON_BASED &&
                (request.cronExpression() == null ||
                        request.cronExpression().isBlank())) {

            throw new TaskValidationException(
                    "Cron expression is required for cron-based schedules"
            );
        }

        if (request.scheduleType() == ScheduleType.RECURRING &&
                request.endTime() == null) {

            throw new TaskValidationException(
                    "End time is required for recurring schedules"
            );
        }

        if (request.maxRetries() != null &&
                request.maxRetries() < 0) {

            throw new TaskValidationException(
                    "Max retries cannot be negative"
            );
        }
    }

    /**
     * Validates an update request against the current task state.
     *
     * @param task    existing scheduled task
     * @param request update request
     */
    public void validateUpdateRequest(
            ScheduledTask task,
            ScheduleUpdateRequest request) {

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new TaskValidationException(
                    "Cannot update task that is in progress"
            );
        }

        if (task.getStatus() == TaskStatus.COMPLETED ||
                task.getStatus() == TaskStatus.CANCELLED) {

            throw new TaskValidationException(
                    "Cannot update task that is completed or cancelled"
            );
        }

        if (request.scheduledTime() != null &&
                request.scheduledTime().isBefore(LocalDateTime.now())) {

            throw new TaskValidationException(
                    "Scheduled time must be in the future"
            );
        }
    }

    /**
     * Converts a status string into TaskStatus.
     *
     * @param status status value
     * @return parsed TaskStatus
     */
    public TaskStatus parseTaskStatus(String status) {

        if (status == null || status.isBlank()) {
            throw new TaskValidationException(
                    "Task status cannot be null or blank"
            );
        }

        try {
            return TaskStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new TaskValidationException(
                    "Invalid task status: " + status
            );
        }
    }

    /**
     * Determines whether a pending task should be triggered immediately.
     *
     * @param task scheduled task
     * @return true when the task should be triggered immediately
     */
    public boolean shouldTriggerImmediately(ScheduledTask task) {

        return task.getScheduledTime()
                .isBefore(LocalDateTime.now().plusMinutes(1))
                && task.getStatus() == TaskStatus.PENDING;
    }
}