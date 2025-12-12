package com.ar.scheduling.config;

import com.ar.scheduling.service.TaskHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class TaskHandlerConfig {

    @Bean
    public Map<String, TaskHandler> taskHandlers(List<TaskHandler> handlers) {
        return handlers.stream()
                .collect(Collectors.toMap(TaskHandler::getTaskType, Function.identity()));
    }
}