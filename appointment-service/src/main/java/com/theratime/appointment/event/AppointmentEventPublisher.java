package com.theratime.appointment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes tenant-aware appointment events to Kafka.
 * Message key is tenant_id so events for the same tenant go to the same partition (ordering per tenant).
 * Best-effort: failures are logged and not propagated so API availability is not tied to Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.appointment-events:appointment.events}")
    private String topic;

    public void publishCreated(AppointmentEventPayload payload) {
        publish(payload);
    }

    public void publishCancelled(AppointmentEventPayload payload) {
        publish(payload);
    }

    public void publishRescheduled(AppointmentEventPayload payload) {
        publish(payload);
    }

    /**
     * Sends a pre-serialized payload to Kafka (used by OutboxPublisher).
     */
    public void sendPayload(String key, String jsonPayload) {
        try {
            kafkaTemplate.send(topic, key, jsonPayload);
            log.debug("Published outbox event to Kafka, key={}", key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to Kafka: " + e.getMessage(), e);
        }
    }

    private void publish(AppointmentEventPayload payload) {
        if (payload.getOccurredAt() == null) {
            payload.setOccurredAt(Instant.now());
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            String key = String.valueOf(payload.getTenantId());
            kafkaTemplate.send(topic, key, json);
            log.debug("Published {} for appointment {} tenant {}", payload.getEventType(), payload.getAppointmentId(), payload.getTenantId());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize appointment event: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to publish appointment event to Kafka: {}", e.getMessage());
        }
    }
}
