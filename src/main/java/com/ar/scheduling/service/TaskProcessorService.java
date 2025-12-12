package com.ar.scheduling.service;

import com.ar.scheduling.entities.ScheduledTask;
import com.ar.scheduling.enums.TaskStatus;
import com.ar.scheduling.exception.TaskNotFoundException;
import com.ar.scheduling.messaging.TaskMessageProducer;
import com.ar.scheduling.repository.ArScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskProcessorService {

    private final ArScheduledTaskRepository scheduledTaskRepository;
    private final TaskMessageProducer taskMessageProducer;
    private final Map<String, TaskHandler> taskHandlers;

    // In-memory store for tracking currently processing tasks
    private final Map<String, Boolean> processingTasks = new ConcurrentHashMap<>();

    /**
     * Process a scheduled task
     */
    public void processTask(String taskId) {
        if (processingTasks.containsKey(taskId)) {
            log.warn("Task {} is already being processed, skipping duplicate execution", taskId);
            return;
        }
        processingTasks.put(taskId, true);
        try {
            ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));
            log.info("Processing task: {} (ID: {})", task.getTaskName(), taskId);

            // Update task status to IN_PROGRESS
            updateTaskStatus(taskId, TaskStatus.IN_PROGRESS, LocalDateTime.now(), null);

            // Execute the actual task
            boolean success = executeTask(task);
            if (success) {
                handleTaskSuccess(taskId);
            } else {
                handleTaskFailure(taskId, "Task execution failed");
            }
        } catch (Exception e) {
            log.error("Error processing task: {}", taskId, e);
            handleTaskFailure(taskId, "Error: " + e.getMessage());
        } finally {
            processingTasks.remove(taskId);
        }
    }

    /**
     * Execute the actual task based on its type
     */
    private boolean executeTask(ScheduledTask task) {
        try {
            String taskType = task.getTaskType();
            TaskHandler handler = taskHandlers.get(taskType);

            if (handler == null) {
                log.error("No handler found for task type: {}", taskType);
                return false;
            }
            log.debug("Executing task {} with handler: {}", task.getTaskId(), taskType);
            return handler.handle(task);
        } catch (Exception e) {
            log.error("Exception during task execution for task: {}", task.getTaskId(), e);
            return false;
        }
    }

    /**
     * Handle successful task execution
     */
    private void handleTaskSuccess(String taskId) {
        try {
            updateTaskStatus(taskId, TaskStatus.COMPLETED, null, LocalDateTime.now());
            taskMessageProducer.sendTaskCompletionNotification(taskId, true, "Task completed successfully");
            log.info("Task completed successfully: {}", taskId);

        } catch (Exception e) {
            log.error("Error handling task success for task: {}", taskId, e);
        }
    }

    /**
     * Handle failed task execution
     */
    private void handleTaskFailure(String taskId, String errorMessage) {
        try {
            ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

            if (task.getRetryCount() < task.getMaxRetries()) {
                // Schedule retry
                handleTaskRetry(task, errorMessage);
            } else {
                // Mark as failed after max retries
                handleTaskFinalFailure(taskId, errorMessage);
            }

        } catch (Exception e) {
            log.error("Error handling task failure for task: {}", taskId, e);
        }
    }

    /**
     * Handle task retry logic
     */
    private void handleTaskRetry(ScheduledTask task, String errorMessage) {
        try {
            String taskId = task.getTaskId();
            int newRetryCount = task.getRetryCount() + 1;

            // Update task for retry
            task.setStatus(TaskStatus.RETRYING);
            task.setRetryCount(newRetryCount);
            task.setErrorMessage("Retry " + newRetryCount + "/" + task.getMaxRetries() + ": " + errorMessage);
            task.setUpdatedAt(LocalDateTime.now());
            scheduledTaskRepository.save(task);

            log.warn("Task {} failed, scheduling retry {}/{}",
                    taskId, newRetryCount, task.getMaxRetries());

            // Send retry message with exponential backoff
            taskMessageProducer.sendRetryMessage(taskId, newRetryCount);

        } catch (Exception e) {
            log.error("Error handling task retry for task: {}", task.getTaskId(), e);
        }
    }

    /**
     * Handle final task failure after max retries
     */
    private void handleTaskFinalFailure(String taskId, String errorMessage) {
        try {
            updateTaskStatus(taskId, TaskStatus.FAILED, null, null, errorMessage);
            taskMessageProducer.sendTaskCompletionNotification(taskId, false, "Task failed: " + errorMessage);
            log.error("Task failed after max retries: {}", taskId);

        } catch (Exception e) {
            log.error("Error handling final task failure for task: {}", taskId, e);
        }
    }

    /**
     * Update task status with execution time
     */
    private void updateTaskStatus(String taskId, TaskStatus status, LocalDateTime executionTime, LocalDateTime completedTime) {
        updateTaskStatus(taskId, status, executionTime, completedTime, null);
    }

    /**
     * Update task status with all details
     */
    private void updateTaskStatus(String taskId, TaskStatus status, LocalDateTime executionTime,
                                  LocalDateTime completedTime, String errorMessage) {
        try {
            ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());

            if (executionTime != null) {
                task.setExecutionTime(executionTime);
            }
            if (completedTime != null) {
                task.setCompletedTime(completedTime);
            }
            if (errorMessage != null) {
                task.setErrorMessage(errorMessage);
            }

            scheduledTaskRepository.save(task);

            // Send status update notification
            taskMessageProducer.sendTaskStatusUpdate(taskId, task.getStatus().name(), status.name());

        } catch (Exception e) {
            log.error("Error updating task status for task: {}", taskId, e);
            throw new RuntimeException("Failed to update task status", e);
        }
    }

    /**
     * Check if a task is currently being processed
     */
    public boolean isTaskProcessing(String taskId) {
        return processingTasks.containsKey(taskId);
    }

    /**
     * Get number of currently processing tasks
     */
    public int getProcessingTaskCount() {
        return processingTasks.size();
    }

    /**
     * Force stop a processing task (for cancellation)
     */
    public boolean stopTaskProcessing(String taskId) {
        if (processingTasks.containsKey(taskId)) {
            processingTasks.remove(taskId);
            log.info("Stopped processing for task: {}", taskId);
            return true;
        }
        return false;
    }
}