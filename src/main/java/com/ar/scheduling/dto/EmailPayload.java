package com.ar.scheduling.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class EmailPayload {

    private String to;
    private String subject;
    private String body;
    private String template;
    private Map<String, Object> variables;
}