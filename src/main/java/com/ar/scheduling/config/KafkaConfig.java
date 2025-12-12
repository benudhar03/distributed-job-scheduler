package com.ar.scheduling.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.task-execution:ar-task-execution-topic}")
    private String taskExecutionTopic;

    @Value("${app.kafka.topics.task-completion:ar-task-completion-topic}")
    private String taskCompletionTopic;

    @Value("${app.kafka.topics.task-status-update:ar-task-status-update-topic}")
    private String taskStatusUpdateTopic;

    @Value("${app.kafka.topics.task-retry:ar-task-retry-topic}")
    private String taskRetryTopic;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32 * 1024); // 32KB
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic taskExecutionTopic() {
        return new NewTopic(taskExecutionTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic taskCompletionTopic() {
        return new NewTopic(taskCompletionTopic, 1, (short) 1);
    }

    @Bean
    public NewTopic taskStatusUpdateTopic() {
        return new NewTopic(taskStatusUpdateTopic, 1, (short) 1);
    }

    @Bean
    public NewTopic taskRetryTopic() {
        return new NewTopic(taskRetryTopic, 3, (short) 1);
    }
}