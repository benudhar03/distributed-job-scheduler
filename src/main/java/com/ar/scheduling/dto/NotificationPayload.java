package com.ar.scheduling.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationPayload {
    private String type; // PUSH, SMS, IN_APP
    private String target;
    private String title;
    private String message;
    private Map<String, Object> data;
}