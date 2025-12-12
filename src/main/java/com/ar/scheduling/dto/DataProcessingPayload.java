package com.ar.scheduling.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;


@Data
@Builder
public class DataProcessingPayload {
    private String dataSource;
    private String operation;
    private Map<String, Object> parameters;
    private int processingTimeMs = 5000;
}