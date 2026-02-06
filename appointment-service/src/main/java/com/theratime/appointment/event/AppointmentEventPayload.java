package com.theratime.appointment.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Tenant-aware appointment event payload for Kafka.
 * All events include tenant_id so consumers can route/filter without schema changes later.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentEventPayload {

    public static final String EVENT_CREATED = "appointment.created";
    public static final String EVENT_CANCELLED = "appointment.cancelled";
    public static final String EVENT_RESCHEDULED = "appointment.rescheduled";

    /** Stable identifier for this logical event (used for idempotency). */
    private String eventId;

    /** Event type: appointment.created, appointment.cancelled, appointment.rescheduled */
    private String eventType;

    /** Tenant (practice) id — always present for tenant-aware consumers */
    private Long tenantId;

    private Long appointmentId;
    private Long userId;
    private Long therapistId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime endTime;

    private String status;

    /** When the event occurred (UTC) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant occurredAt;
}
