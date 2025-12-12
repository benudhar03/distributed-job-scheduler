package com.ar.scheduling.service;

import com.ar.scheduling.dto.ScheduleRequest;
import com.ar.scheduling.dto.ScheduleResponse;
import com.ar.scheduling.dto.ScheduleUpdateRequest;
import com.ar.scheduling.dto.TaskStatusResponse;
import com.ar.scheduling.entities.ScheduledTask;
import com.ar.scheduling.enums.ScheduleType;
import com.ar.scheduling.enums.TaskStatus;
import com.ar.scheduling.exception.TaskNotFoundException;
import com.ar.scheduling.exception.TaskValidationException;
import com.ar.scheduling.mapper.ArSchedulingTaskMapper;
import com.ar.scheduling.messaging.TaskMessageProducer;
import com.ar.scheduling.repository.ArScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArSchedulingService {

    private final ArScheduledTaskRepository scheduledTaskRepository;
    private final TaskMessageProducer taskMessageProducer;
    private final TaskProcessorService taskProcessorService;
    private final ArSchedulingTaskMapper schedulingTaskMapper;

    @Transactional
    public ScheduleResponse scheduleTask(ScheduleRequest request) {
        log.info("Scheduling new task: {}", request.taskName());
        validateScheduleRequest(request);
        validateTaskNameUniqueness(request);

        ScheduledTask task = schedulingTaskMapper.createTaskFromRequest(request);
        ScheduledTask savedTask = scheduledTaskRepository.save(task);

        log.info("Task scheduled successfully with ID: {}", savedTask.getTaskId());

        // If it's an immediate task, trigger execution
        if (shouldTriggerImmediately(savedTask)) {
            triggerTaskExecution(savedTask.getTaskId());
        }
        return schedulingTaskMapper.mapToScheduleResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getScheduledTask(String taskId) {
        log.debug("Fetching task with ID: {}", taskId);

        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
        return schedulingTaskMapper.mapToScheduleResponse(task);
    }

    @Transactional
    public void deleteScheduledTask(String taskId) {
        log.info("Deleting task with ID: {}", taskId);

        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new TaskValidationException("Cannot delete task that is in progress");
        }
        scheduledTaskRepository.delete(task);
        log.info("Task deleted successfully: {}", taskId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getScheduledTasksByStatus(String status, int page, int size) {
        log.debug("Fetching tasks with status: {}, page: {}, size: {}", status, page, size);

        TaskStatus taskStatus = validateAndParseStatus(status);
        Pageable pageable = PageRequest.of(page, size);

        Page<ScheduledTask> tasks = scheduledTaskRepository.findByStatus(taskStatus, pageable);
        return tasks.getContent()
                .stream()
                .map(schedulingTaskMapper::mapToScheduleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskStatusResponse getScheduledTaskStatus(String taskId) {
        log.debug("Fetching status for task: {}", taskId);

        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
        return schedulingTaskMapper.mapToTaskStatusResponse(task);
    }

    @Transactional
    public TaskStatusResponse cancelScheduledTask(String taskId) {
        log.info("Cancelling task with ID: {}", taskId);

        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.FAILED) {
            throw new TaskValidationException("Cannot cancel task that is already completed or failed");
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            // Try to stop the processing task
            boolean stopped = taskProcessorService.stopTaskProcessing(taskId);
            if (!stopped) {
                throw new TaskValidationException("Cannot cancel task that is in progress");
            }
        }
        task.setStatus(TaskStatus.CANCELLED);
        task.setUpdatedAt(LocalDateTime.now());
        ScheduledTask updatedTask = scheduledTaskRepository.save(task);

        // Send status update notification
        taskMessageProducer.sendTaskStatusUpdate(taskId, task.getStatus().name(), TaskStatus.CANCELLED.name());

        log.info("Task cancelled successfully: {}", taskId);
        return schedulingTaskMapper.mapToTaskStatusResponse(updatedTask);
    }


    @Transactional
    public ScheduleResponse updateScheduleRequest(String taskId, ScheduleUpdateRequest request) {
        log.info("Updating task with ID: {}", taskId);

        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
        validateUpdateRequest(task, request);
        String oldStatus = task.getStatus().name();

        // Update task fields
        if (request.scheduledTime() != null) {
            task.setScheduledTime(request.scheduledTime());
        }
        if (request.payload() != null) {
            task.setPayload(request.payload());
        }
        if (request.metadata() != null) {
            task.setMetadata(request.metadata());
        }
        if (request.cronExpression() != null) {
            task.setCronExpression(request.cronExpression());
        }
        if (request.endTime() != null) {
            task.setEndTime(request.endTime());
        }
        if (request.maxRetries() != null) {
            task.setMaxRetries(request.maxRetries());
        }
        task.setUpdatedAt(LocalDateTime.now());
        ScheduledTask updatedTask = scheduledTaskRepository.save(task);

        // Send status update if status changed
        if (!oldStatus.equals(updatedTask.getStatus().name())) {
            taskMessageProducer.sendTaskStatusUpdate(taskId, oldStatus, updatedTask.getStatus().name());
        }

        log.info("Task updated successfully: {}", taskId);
        return schedulingTaskMapper.mapToScheduleResponse(updatedTask);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllScheduledTasks(int page, int size) {
        log.debug("Fetching all tasks, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<ScheduledTask> tasks = scheduledTaskRepository.findAll(pageable);

        return tasks.getContent()
                .stream()
                .map(schedulingTaskMapper::mapToScheduleResponse)
                .toList();
    }

    @Transactional
    public TaskStatusResponse triggerTask(String taskId) {
        log.info("Triggering task execution for ID: {}", taskId);

        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new TaskValidationException("Task is already in progress");
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new TaskValidationException("Task is already completed");
        }

        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new TaskValidationException("Cannot trigger cancelled task");
        }

        // Update task status and trigger execution
        String oldStatus = task.getStatus().name();
        task.setStatus(TaskStatus.SCHEDULED);
        task.setScheduledTime(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        ScheduledTask updatedTask = scheduledTaskRepository.save(task);

        // Send status update
        taskMessageProducer.sendTaskStatusUpdate(taskId, oldStatus, TaskStatus.SCHEDULED.name());

        // Trigger immediate execution
        triggerTaskExecution(taskId);

        log.info("Task triggered successfully: {}", taskId);
        return schedulingTaskMapper.mapToTaskStatusResponse(updatedTask);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getTasksByType(String taskType) {
        log.debug("Fetching tasks by type: {}", taskType);
        List<ScheduledTask> tasks = scheduledTaskRepository.findByTaskTypeAndStatus(taskType, TaskStatus.PENDING);
        return tasks.stream()
                .map(schedulingTaskMapper::mapToScheduleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getOverdueTasks() {
        log.debug("Fetching overdue tasks");
        List<ScheduledTask> tasks = scheduledTaskRepository.findOverdueTasks(LocalDateTime.now());

        return tasks.stream()
                .map(schedulingTaskMapper::mapToScheduleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getTaskCountByStatus(String status) {
        TaskStatus taskStatus = validateAndParseStatus(status);
        return scheduledTaskRepository.countByStatus(taskStatus);
    }

    private void validateScheduleRequest(ScheduleRequest request) {
        if (request.scheduledTime().isBefore(LocalDateTime.now())) {
            throw new TaskValidationException("Scheduled time must be in the future");
        }
        if (request.scheduleType() == ScheduleType.CRON_BASED &&
                (request.cronExpression() == null || request.cronExpression().isBlank())) {
            throw new TaskValidationException("Cron expression is required for cron-based schedules");
        }
        if (request.scheduleType() == ScheduleType.RECURRING && request.endTime() == null) {
            throw new TaskValidationException("End time is required for recurring schedules");
        }
        if (request.maxRetries() != null && request.maxRetries() < 0) {
            throw new TaskValidationException("Max retries cannot be negative");
        }
    }

    private void validateUpdateRequest(ScheduledTask task, ScheduleUpdateRequest request) {
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            throw new TaskValidationException("Cannot update task that is in progress");
        }
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new TaskValidationException("Cannot update task that is completed or cancelled");
        }
        if (request.scheduledTime() != null && request.scheduledTime().isBefore(LocalDateTime.now())) {
            throw new TaskValidationException("Scheduled time must be in the future");
        }
    }

    private TaskStatus validateAndParseStatus(String status) {
        try {
            return TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TaskValidationException("Invalid task status: " + status);
        }
    }

    private boolean shouldTriggerImmediately(ScheduledTask task) {
        return task.getScheduledTime().isBefore(LocalDateTime.now().plusMinutes(1)) &&
                task.getStatus() == TaskStatus.PENDING;
    }

    @Async
    public void triggerTaskExecution(String taskId) {
        try {
            taskProcessorService.processTask(taskId);
            log.debug("Task execution message sent for task: {}", taskId);
        } catch (Exception e) {
            log.error("Failed to send task execution message for task: {}", taskId, e);
            // Update task status to failed if message sending fails
            scheduledTaskRepository.findByTaskId(taskId).ifPresent(task -> {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("Failed to trigger execution: " + e.getMessage());
                task.setUpdatedAt(LocalDateTime.now());
                scheduledTaskRepository.save(task);
            });
        }
    }

    private void validateTaskNameUniqueness(ScheduleRequest request) {
        List<TaskStatus> excludedStatuses = Arrays.asList(TaskStatus.CANCELLED, TaskStatus.COMPLETED);
        if (scheduledTaskRepository.existsByTaskNameAndStatusNotIn(request.taskName(), excludedStatuses)) {
            throw new TaskValidationException("Task with name '" + request.taskName() + "' already exists and is active");
        }
        if (request.taskType() != null && !request.taskType().isBlank()) {
            if (scheduledTaskRepository.existsByTaskNameAndTaskTypeAndStatusNotIn(
                    request.taskName(), request.taskType(), excludedStatuses)) {
                throw new TaskValidationException("Task with name '" + request.taskName() +
                        "' and type '" + request.taskType() + "' already exists");
            }
        }
    }
}