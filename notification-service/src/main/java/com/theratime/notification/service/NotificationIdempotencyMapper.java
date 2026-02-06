package com.theratime.notification.service;

import com.theratime.notification.entity.ProcessedEventEntity;
import com.theratime.notification.event.AppointmentEventPayload;

public final class NotificationIdempotencyMapper {

    private NotificationIdempotencyMapper() {
    }

    public static ProcessedEventEntity toEntity(String eventId, AppointmentEventPayload event) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setTenantId(event.getTenantId());
        return entity;
    }
}

