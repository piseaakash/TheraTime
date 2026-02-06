package com.theratime.notification.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Appointment event payload (same shape as appointment-service).
 * Consumed from Kafka; tenant_id is always present.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointmentEventPayload {

    public static final String EVENT_CREATED = "appointment.created";
    public static final String EVENT_CANCELLED = "appointment.cancelled";
    public static final String EVENT_RESCHEDULED = "appointment.rescheduled";

    /** Stable identifier for this logical event (used for idempotency). */
    private String eventId;

    private String eventType;
    private Long tenantId;
    private Long appointmentId;
    private Long userId;
    private Long therapistId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime endTime;

    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant occurredAt;
}
