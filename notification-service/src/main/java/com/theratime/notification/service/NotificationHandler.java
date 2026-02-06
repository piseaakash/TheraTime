package com.theratime.notification.service;

import com.theratime.notification.config.NotificationConfigResolver;
import com.theratime.notification.entity.NotificationConfigEntity;
import com.theratime.notification.event.AppointmentEventPayload;
import com.theratime.notification.repository.ProcessedEventRepository;
import com.theratime.notification.send.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHandler {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationConfigResolver configResolver;
    private final EmailSender emailSender;
    private final WhatsAppSender whatsAppSender;
    private final ProcessedEventRepository processedEventRepository;

    public void handle(AppointmentEventPayload event) {
        String eventId = buildEventId(event);
        boolean alreadyProcessed = processedEventRepository.existsById(eventId);
        if (alreadyProcessed) {
            log.debug("Skipping already processed event {}", eventId);
            return;
        }

        Optional<NotificationConfigEntity> configOpt = configResolver.resolve(event.getTenantId(), event.getTherapistId());
        if (configOpt.isEmpty()) {
            log.debug("No notification config for tenant {} therapist {}", event.getTenantId(), event.getTherapistId());
            return;
        }
        NotificationConfigEntity config = configOpt.get();
        String subject = buildSubject(event);
        String body = buildBody(event);
        String message = subject + "\n" + body;

        String toEmail = config.getDefaultToEmail();
        String toPhone = config.getDefaultToPhone();
        if (toEmail == null && toPhone == null) {
            log.debug("No default_to_email or default_to_phone for tenant {}; skipping send", event.getTenantId());
            return;
        }

        if (Boolean.TRUE.equals(config.getEmailEnabled()) && toEmail != null) {
            TenantMailConfig mailConfig = TenantMailConfig.builder()
                    .from(config.getEmailFrom())
                    .smtpHost(config.getSmtpHost())
                    .smtpPort(config.getSmtpPort())
                    .smtpUsername(config.getSmtpUsername())
                    .smtpPassword(config.getSmtpPasswordEncrypted())
                    .build();
            emailSender.send(EmailRequest.builder().to(toEmail).subject(subject).body(body).build(), mailConfig);
        }

        if (Boolean.TRUE.equals(config.getWhatsappEnabled()) && toPhone != null && config.getWhatsappPhoneOrApiKey() != null) {
            TenantWhatsAppConfig waConfig = TenantWhatsAppConfig.builder()
                    .phoneOrApiKey(config.getWhatsappPhoneOrApiKey())
                    .build();
            whatsAppSender.send(WhatsAppRequest.builder().toPhone(toPhone).message(message).build(), waConfig);
        }

        processedEventRepository.save(NotificationIdempotencyMapper.toEntity(eventId, event));
    }

    private String buildEventId(AppointmentEventPayload event) {
        if (event.getEventId() != null && !event.getEventId().isBlank()) {
            return event.getEventId();
        }
        String eventType = event.getEventType() != null ? event.getEventType() : "";
        String occurredAt = event.getOccurredAt() != null ? event.getOccurredAt().toString() : "";
        return event.getTenantId() + ":" + event.getAppointmentId() + ":" + eventType + ":" + occurredAt;
    }

    private String buildSubject(AppointmentEventPayload event) {
        return switch (event.getEventType() != null ? event.getEventType() : "") {
            case AppointmentEventPayload.EVENT_CREATED -> "Appointment booked";
            case AppointmentEventPayload.EVENT_CANCELLED -> "Appointment cancelled";
            case AppointmentEventPayload.EVENT_RESCHEDULED -> "Appointment rescheduled";
            default -> "Appointment update";
        };
    }

    private String buildBody(AppointmentEventPayload event) {
        String start = event.getStartTime() != null ? event.getStartTime().format(DATE_TIME) : "-";
        String end = event.getEndTime() != null ? event.getEndTime().format(DATE_TIME) : "-";
        return String.format("Appointment id: %s | %s - %s | Therapist: %s | User: %s",
                event.getAppointmentId(), start, end, event.getTherapistId(), event.getUserId());
    }
}
