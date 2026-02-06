package com.theratime.notification.service;

import com.theratime.notification.entity.ProcessedEventEntity;
import com.theratime.notification.event.AppointmentEventPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationIdempotencyMapperTest {

    @Test
    void toEntity_mapsEventIdAndTenantId() {
        String eventId = "evt-123";
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId(eventId)
                .tenantId(5L)
                .appointmentId(10L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .build();

        ProcessedEventEntity entity = NotificationIdempotencyMapper.toEntity(eventId, payload);

        assertThat(entity.getEventId()).isEqualTo(eventId);
        assertThat(entity.getTenantId()).isEqualTo(5L);
    }

    @Test
    void toEntity_withNullTenantId_setsTenantIdNull() {
        String eventId = "evt-null-tenant";
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId(eventId)
                .tenantId(null)
                .appointmentId(10L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .build();

        ProcessedEventEntity entity = NotificationIdempotencyMapper.toEntity(eventId, payload);

        assertThat(entity.getEventId()).isEqualTo(eventId);
        assertThat(entity.getTenantId()).isNull();
    }
}
