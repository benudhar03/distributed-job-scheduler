package com.ar.scheduling.service;

import com.ar.scheduling.dto.DataProcessingPayload;
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
public class DataProcessingTaskHandler implements TaskHandler {

    private final ObjectMapper objectMapper;

    @Override
    public String getTaskType() {
        return "DATA_PROCESSING";
    }

    @Override
    public boolean handle(ScheduledTask task) {
        try {
            log.info("Processing data processing task: {}", task.getTaskId());
            
            // Parse payload
            DataProcessingPayload payload = parsePayload(task.getPayload());
            
            // Simulate data processing
            log.info("Processing data from source: {}, Operation: {}", 
                    payload.getDataSource(), payload.getOperation());
            
            // Simulate processing time based on data size
            Thread.sleep(payload.getProcessingTimeMs());
            
            log.info("Data processing completed for task: {}", task.getTaskId());
            return true;
            
        } catch (Exception e) {
            log.error("Error processing data task: {}", task.getTaskId(), e);
            return false;
        }
    }

    private DataProcessingPayload parsePayload(String payloadJson) throws JsonProcessingException {
        return objectMapper.readValue(payloadJson, DataProcessingPayload.class);
    }

}