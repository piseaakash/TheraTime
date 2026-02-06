package com.theratime.appointment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.OutboxEntity;
import com.theratime.appointment.event.AppointmentEventPayload;
import com.theratime.appointment.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Enqueues appointment events into the transactional outbox (same DB transaction as appointment save).
 * Ensures no lost events when Kafka is down.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void enqueueEvent(Long tenantId, Appointment appointment, String eventType) {
        String eventId = UUID.randomUUID().toString();
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId(eventId)
                .eventType(eventType)
                .tenantId(tenantId)
                .appointmentId(appointment.getId())
                .userId(appointment.getUserId())
                .therapistId(appointment.getTherapistId())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus() != null ? appointment.getStatus().name() : null)
                .occurredAt(Instant.now())
                .build();
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEntity outbox = OutboxEntity.builder()
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxEntity.STATUS_PENDING)
                    .attemptCount(0)
                    .build();
            outboxRepository.save(outbox);
            log.debug("Enqueued outbox event {} for appointment {} tenant {}", eventType, appointment.getId(), tenantId);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event for outbox: {}", e.getMessage());
        }
    }
}
