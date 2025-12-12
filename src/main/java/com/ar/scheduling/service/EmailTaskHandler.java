package com.ar.scheduling.service;

import com.ar.scheduling.dto.EmailPayload;
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
public class EmailTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper;

    @Override
    public String getTaskType() {
        return "EMAIL";
    }

    @Override
    public boolean handle(ScheduledTask task) {
        try {
            log.info("Processing email task: {}", task.getTaskId());
            
            // Parse payload
            EmailPayload payload = parsePayload(task.getPayload());
            
            // Simulate email sending
            log.info("Sending email to: {}, Subject: {}, Body: {}", 
                    payload.getTo(), payload.getSubject(), payload.getBody());
            
            // Simulate processing time
            Thread.sleep(2000);
            
            log.info("Email sent successfully for task: {}", task.getTaskId());
            return true;
            
        } catch (Exception e) {
            log.error("Error processing email task: {}", task.getTaskId(), e);
            return false;
        }
    }

    private EmailPayload parsePayload(String payloadJson) throws JsonProcessingException {
        return objectMapper.readValue(payloadJson, EmailPayload.class);
    }


}