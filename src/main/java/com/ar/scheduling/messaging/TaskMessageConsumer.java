package com.ar.scheduling.messaging;

import com.ar.scheduling.dto.kafka.TaskExecutionMessage;
import com.ar.scheduling.service.TaskProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageConsumer {

    private final TaskProcessorService taskProcessorService;

    @KafkaListener(
            topics = "${app.kafka.topics.task-execution:ar-task-execution-topic}",
            groupId = "${app.kafka.consumer-group:ar-task-processing-group}",
            concurrency = "3"
    )
    public void consumeTaskExecutionMessage(
            @Payload TaskExecutionMessage message,
            @Header("messageType") String messageType,
            @Header("timestamp") Long timestamp,
            Acknowledgment ack) {

        log.info("Received task execution message for task: {}, type: {}",
                message.getTaskId(), messageType);

        try {
            taskProcessorService.processTask(message.getTaskId());
            ack.acknowledge();
            log.info("Successfully processed task execution message for task: {}", message.getTaskId());
        } catch (Exception e) {
            log.error("Error processing task execution message for task: {}", message.getTaskId(), e);
            // Don't acknowledge to trigger retry via Kafka
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.task-retry:ar-task-retry-topic}",
            groupId = "${app.kafka.consumer-group:ar-task-processing-group}",
            concurrency = "2"
    )
    public void consumeTaskRetryMessage(
            @Payload TaskExecutionMessage message,
            @Header("retryCount") Integer retryCount,
            @Header("retryDelay") Long retryDelay,
            Acknowledgment ack) {

        log.info("Received retry message for task: {}, retry count: {}",
                message.getTaskId(), retryCount);

        try {
            taskProcessorService.processTask(message.getTaskId());
            ack.acknowledge();
            log.info("Successfully processed retry message for task: {}", message.getTaskId());
        } catch (Exception e) {
            log.error("Error processing retry message for task: {}", message.getTaskId(), e);
            // Don't acknowledge to trigger further retry
        }
    }
}