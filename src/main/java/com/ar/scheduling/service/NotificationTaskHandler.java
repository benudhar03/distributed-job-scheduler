package com.ar.scheduling.service;

import com.ar.scheduling.dto.NotificationPayload;
import com.ar.scheduling.entities.ScheduledTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper;

    @Override
    public String getTaskType() {
        return "NOTIFICATION";
    }

    @Override
    public boolean handle(ScheduledTask task) {
        try {
            log.info("Processing notification task: {}", task.getTaskId());
            
            // Parse payload
            NotificationPayload payload = parsePayload(task.getPayload());
            
            // Simulate notification sending
            log.info("Sending {} notification to: {}, Title: {}", 
                    payload.getType(), payload.getTarget(), payload.getTitle());
            
            // Simulate processing time
            Thread.sleep(1000);
            
            log.info("Notification sent successfully for task: {}", task.getTaskId());
            return true;
            
        } catch (Exception e) {
            log.error("Error processing notification task: {}", task.getTaskId(), e);
            return false;
        }
    }

    private NotificationPayload parsePayload(String payloadJson) throws JsonProcessingException {
        return objectMapper.readValue(payloadJson, NotificationPayload.class);
    }

}