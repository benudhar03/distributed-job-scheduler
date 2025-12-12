package com.ar.scheduling.messaging;

import com.ar.scheduling.dto.kafka.TaskCompletionMessage;
import com.ar.scheduling.dto.kafka.TaskExecutionMessage;
import com.ar.scheduling.entities.ScheduledTask;
import com.ar.scheduling.repository.ArScheduledTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ArScheduledTaskRepository scheduledTaskRepository;

    @Value("${app.kafka.topics.task-execution:ar-task-execution-topic}")
    private String taskExecutionTopic;

    @Value("${app.kafka.topics.task-completion:ar-task-completion-topic}")
    private String taskCompletionTopic;

    @Value("${app.kafka.topics.task-status-update:ar-task-status-update-topic}")
    private String taskStatusUpdateTopic;

    @Value("${app.kafka.topics.task-retry:ar-task-retry-topic}")
    private String taskRetryTopic;

    @Value("${app.kafka.max-retry-attempts:3}")
    private int maxRetryAttempts;

    /**
     * Send immediate task execution message
     */
    public void sendTaskExecutionMessage(String taskId) {
        try {
            TaskExecutionMessage message = createTaskExecutionMessage(taskId);
            Message<TaskExecutionMessage> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, taskExecutionTopic)
                    .setHeader(KafkaHeaders.KEY, taskId)
                    .setHeader("messageType", "TASK_EXECUTION")
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(kafkaMessage);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Task execution message sent successfully for task: {} to partition: {}",
                            taskId, result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to send task execution message for task: {}", taskId, ex);
                    updateTaskOnMessagingError(taskId, "Kafka error: " + ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Error while sending task execution message for task: {}", taskId, e);
            updateTaskOnMessagingError(taskId, "Message sending error: " + e.getMessage());
            throw new MessagingException("Failed to send task execution message", e);
        }
    }

    /**
     * Send delayed task execution message using Kafka headers (simulated delay)
     */
    public void sendDelayedTaskExecutionMessage(String taskId, long delayInMillis) {
        try {
            TaskExecutionMessage message = createTaskExecutionMessage(taskId);

            Message<TaskExecutionMessage> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, taskExecutionTopic)
                    .setHeader(KafkaHeaders.KEY, taskId)
                    .setHeader("messageType", "DELAYED_TASK_EXECUTION")
                    .setHeader("scheduledTime", System.currentTimeMillis() + delayInMillis)
                    .setHeader("originalTimestamp", System.currentTimeMillis())
                    .setHeader("delayInMillis", delayInMillis)
                    .build();

            kafkaTemplate.send(kafkaMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Delayed task execution message sent for task: {} with delay: {} ms",
                                    taskId, delayInMillis);
                        } else {
                            log.error("Failed to send delayed task message for task: {}", taskId, ex);
                            updateTaskOnMessagingError(taskId, "Delayed message error: " + ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending delayed task message for task: {}", taskId, e);
            updateTaskOnMessagingError(taskId, "Delayed message error: " + e.getMessage());
            throw new MessagingException("Failed to send delayed task message", e);
        }
    }

    /**
     * Send retry message for failed task execution with exponential backoff
     */
    public void sendRetryMessage(String taskId, int retryCount) {
        try {
            if (retryCount >= maxRetryAttempts) {
                log.warn("Max retry attempts reached for task: {}, not sending retry message", taskId);
                return;
            }

            TaskExecutionMessage message = createTaskExecutionMessage(taskId);
            long retryDelay = calculateRetryDelay(retryCount);

            Message<TaskExecutionMessage> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, taskRetryTopic)
                    .setHeader(KafkaHeaders.KEY, taskId)
                    .setHeader("messageType", "TASK_RETRY")
                    .setHeader("retryCount", retryCount)
                    .setHeader("retryDelay", retryDelay)
                    .setHeader("scheduledTime", System.currentTimeMillis() + retryDelay)
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(kafkaMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Retry message sent for task: {}, retry count: {}, delay: {} ms",
                                    taskId, retryCount, retryDelay);
                        } else {
                            log.error("Failed to send retry message for task: {}", taskId, ex);
                            updateTaskOnMessagingError(taskId, "Retry message error: " + ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending retry message for task: {}", taskId, e);
            updateTaskOnMessagingError(taskId, "Retry message error: " + e.getMessage());
            throw new MessagingException("Failed to send retry message", e);
        }
    }

    /**
     * Send bulk task execution messages
     */
    public void sendBulkTaskExecutionMessages(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            log.warn("No task IDs provided for bulk message sending");
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (String taskId : taskIds) {
            try {
                sendTaskExecutionMessage(taskId);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send message for task: {} in bulk operation", taskId, e);
                failureCount++;
            }
        }

        log.info("Bulk message sending completed - Success: {}, Failed: {}, Total: {}",
                successCount, failureCount, taskIds.size());
    }

    /**
     * Send task completion notification
     */
    public void sendTaskCompletionNotification(String taskId, boolean success, String message) {
        try {
            TaskCompletionMessage completionMessage = TaskCompletionMessage.builder()
                    .taskId(taskId)
                    .success(success)
                    .message(message)
                    .completionTime(LocalDateTime.now())
                    .build();

            Message<TaskCompletionMessage> kafkaMessage = MessageBuilder
                    .withPayload(completionMessage)
                    .setHeader(KafkaHeaders.TOPIC, taskCompletionTopic)
                    .setHeader(KafkaHeaders.KEY, taskId)
                    .setHeader("messageType", "TASK_COMPLETION")
                    .setHeader("success", success)
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(kafkaMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Task completion notification sent for task: {}, success: {}", taskId, success);
                        } else {
                            log.error("Failed to send task completion notification for task: {}", taskId, ex);
                            // Don't throw exception for notification failures as they're non-critical
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending task completion notification for task: {}", taskId, e);
            // Don't throw exception for notification failures as they're non-critical
        }
    }

    /**
     * Send task status update message
     */
    public void sendTaskStatusUpdate(String taskId, String oldStatus, String newStatus) {
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("taskId", taskId);
            statusUpdate.put("oldStatus", oldStatus);
            statusUpdate.put("newStatus", newStatus);
            statusUpdate.put("updateTime", LocalDateTime.now());

            Message<Map<String, Object>> kafkaMessage = MessageBuilder
                    .withPayload(statusUpdate)
                    .setHeader(KafkaHeaders.TOPIC, taskStatusUpdateTopic)
                    .setHeader(KafkaHeaders.KEY, taskId)
                    .setHeader("messageType", "TASK_STATUS_UPDATE")
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(kafkaMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Task status update sent for task: {} - {} -> {}", taskId, oldStatus, newStatus);
                        } else {
                            log.error("Failed to send task status update for task: {}", taskId, ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending task status update for task: {}", taskId, e);
            // Don't throw exception for notification failures
        }
    }

    /**
     * Send message with callback for synchronous processing
     */
    public boolean sendTaskExecutionMessageSync(String taskId) {
        try {
            TaskExecutionMessage message = createTaskExecutionMessage(taskId);

            Message<TaskExecutionMessage> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, taskExecutionTopic)
                    .setHeader(KafkaHeaders.KEY, taskId)
                    .setHeader("messageType", "TASK_EXECUTION")
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            SendResult<String, Object> result = kafkaTemplate.send(kafkaMessage).get();
            log.info("Task execution message sent synchronously for task: {} to partition: {}",
                    taskId, result.getRecordMetadata().partition());
            return true;

        } catch (Exception e) {
            log.error("Error sending task execution message synchronously for task: {}", taskId, e);
            updateTaskOnMessagingError(taskId, "Sync message error: " + e.getMessage());
            return false;
        }
    }

    // Private helper methods

    private TaskExecutionMessage createTaskExecutionMessage(String taskId) {
        ScheduledTask task = scheduledTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new MessagingException("Task not found: " + taskId));

        return TaskExecutionMessage.builder()
                .taskId(taskId)
                .taskName(task.getTaskName())
                .taskType(task.getTaskType())
                .scheduleType(task.getScheduleType().name())
                .payload(task.getPayload())
                .metadata(task.getMetadata())
                .scheduledTime(task.getScheduledTime())
                .retryCount(task.getRetryCount())
                .maxRetries(task.getMaxRetries())
                .priority(task.getPriority())
                .createdBy(task.getCreatedBy())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private long calculateRetryDelay(int retryCount) {
        // Exponential backoff: 5^retryCount seconds (5s, 25s, 125s, etc.) with max of 10 minutes
        long delay = (long) Math.pow(5, retryCount + 1) * 1000;
        return Math.min(delay, 10 * 60 * 1000); // Max 10 minutes
    }

    private void updateTaskOnMessagingError(String taskId, String errorMessage) {
        try {
            scheduledTaskRepository.findByTaskId(taskId).ifPresent(task -> {
                task.setErrorMessage("Messaging error: " + errorMessage);
                task.setUpdatedAt(LocalDateTime.now());
                scheduledTaskRepository.save(task);
            });
        } catch (Exception e) {
            log.error("Failed to update task on messaging error for task: {}", taskId, e);
        }
    }

}