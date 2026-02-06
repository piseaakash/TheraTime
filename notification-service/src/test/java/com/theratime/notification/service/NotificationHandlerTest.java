package com.theratime.notification.service;

import com.theratime.notification.config.NotificationConfigResolver;
import com.theratime.notification.entity.NotificationConfigEntity;
import com.theratime.notification.event.AppointmentEventPayload;
import com.theratime.notification.repository.ProcessedEventRepository;
import com.theratime.notification.send.EmailRequest;
import com.theratime.notification.send.EmailSender;
import com.theratime.notification.send.TenantMailConfig;
import com.theratime.notification.send.WhatsAppRequest;
import com.theratime.notification.send.WhatsAppSender;
import com.theratime.notification.send.TenantWhatsAppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationHandlerTest {

    @Mock
    private NotificationConfigResolver configResolver;
    @Mock
    private EmailSender emailSender;
    @Mock
    private WhatsAppSender whatsAppSender;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private NotificationHandler notificationHandler;

    @Test
    void handle_whenAlreadyProcessed_skipsSending() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-1")
                .tenantId(1L)
                .appointmentId(10L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        when(processedEventRepository.existsById("evt-1")).thenReturn(true);

        notificationHandler.handle(payload);

        verifyNoInteractions(configResolver, emailSender, whatsAppSender);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handle_whenNoConfig_skipsSendingAndSave() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-no-config")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        when(processedEventRepository.existsById("evt-no-config")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.empty());

        notificationHandler.handle(payload);

        verifyNoInteractions(emailSender, whatsAppSender);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handle_whenNoToEmailOrToPhone_skipsSendingAndSave() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-no-to")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .whatsappEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail(null)
                .defaultToPhone(null)
                .build();

        when(processedEventRepository.existsById("evt-no-to")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verifyNoInteractions(emailSender, whatsAppSender);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handle_sendsEmailAndPersistsIdempotency() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-2")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .userId(3L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById("evt-2")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        ArgumentCaptor<TenantMailConfig> configCaptor = ArgumentCaptor.forClass(TenantMailConfig.class);
        verify(emailSender).send(emailCaptor.capture(), configCaptor.capture());
        assertThat(emailCaptor.getValue().getSubject()).isEqualTo("Appointment booked");

        verify(processedEventRepository).save(any());
    }

    @Test
    void handle_sendsWhatsAppOnlyAndPersistsIdempotency() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-wa")
                .tenantId(2L)
                .appointmentId(20L)
                .therapistId(6L)
                .userId(4L)
                .eventType(AppointmentEventPayload.EVENT_CANCELLED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(2L)
                .emailEnabled(false)
                .whatsappEnabled(true)
                .whatsappPhoneOrApiKey("api-key")
                .defaultToPhone("+1234567890")
                .build();

        when(processedEventRepository.existsById("evt-wa")).thenReturn(false);
        when(configResolver.resolve(2L, 6L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verifyNoInteractions(emailSender);
        ArgumentCaptor<WhatsAppRequest> waCaptor = ArgumentCaptor.forClass(WhatsAppRequest.class);
        ArgumentCaptor<TenantWhatsAppConfig> waConfigCaptor = ArgumentCaptor.forClass(TenantWhatsAppConfig.class);
        verify(whatsAppSender).send(waCaptor.capture(), waConfigCaptor.capture());
        assertThat(waCaptor.getValue().getToPhone()).isEqualTo("+1234567890");
        assertThat(waCaptor.getValue().getMessage()).contains("Appointment cancelled");
        verify(processedEventRepository).save(any());
    }

    @Test
    void handle_sendsEmailAndWhatsAppAndPersistsIdempotency() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-both")
                .tenantId(1L)
                .appointmentId(11L)
                .therapistId(5L)
                .userId(3L)
                .eventType(AppointmentEventPayload.EVENT_RESCHEDULED)
                .startTime(LocalDateTime.of(2025, 6, 1, 10, 0))
                .endTime(LocalDateTime.of(2025, 6, 1, 11, 0))
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("email@example.com")
                .whatsappEnabled(true)
                .whatsappPhoneOrApiKey("key")
                .defaultToPhone("+9876543210")
                .build();

        when(processedEventRepository.existsById("evt-both")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailSender).send(emailCaptor.capture(), any(TenantMailConfig.class));
        assertThat(emailCaptor.getValue().getSubject()).isEqualTo("Appointment rescheduled");
        assertThat(emailCaptor.getValue().getBody()).contains("2025-06-01 10:00");
        assertThat(emailCaptor.getValue().getBody()).contains("-");

        ArgumentCaptor<WhatsAppRequest> waCaptor = ArgumentCaptor.forClass(WhatsAppRequest.class);
        verify(whatsAppSender).send(waCaptor.capture(), any(TenantWhatsAppConfig.class));
        assertThat(waCaptor.getValue().getMessage()).contains("Appointment rescheduled");

        verify(processedEventRepository).save(any());
    }

    @Test
    void handle_buildEventIdUsesPayloadEventIdWhenPresent() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("custom-id-123")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById("custom-id-123")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verify(processedEventRepository).save(argThat(entity ->
                "custom-id-123".equals(entity.getEventId())));
    }

    @Test
    void handle_buildEventIdFallbackWhenEventIdNullOrBlank() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId(null)
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.parse("2025-01-15T10:00:00Z"))
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<com.theratime.notification.entity.ProcessedEventEntity> saveCaptor =
                ArgumentCaptor.forClass(com.theratime.notification.entity.ProcessedEventEntity.class);
        verify(processedEventRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getEventId()).startsWith("1:10:appointment.created:");
        assertThat(saveCaptor.getValue().getEventId()).contains("2025-01-15");
    }

    @Test
    void handle_buildSubjectDefaultForUnknownEventType() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-unknown")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .userId(3L)
                .eventType("unknown.type")
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById("evt-unknown")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailSender).send(emailCaptor.capture(), any(TenantMailConfig.class));
        assertThat(emailCaptor.getValue().getSubject()).isEqualTo("Appointment update");
    }

    @Test
    void handle_buildBodyWithNullStartEndTimes() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-null-times")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .userId(3L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .startTime(null)
                .endTime(null)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById("evt-null-times")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailSender).send(emailCaptor.capture(), any(TenantMailConfig.class));
        assertThat(emailCaptor.getValue().getBody()).contains("-");
        assertThat(emailCaptor.getValue().getBody()).contains("Appointment id: 10");
    }

    // --- Branches: emailEnabled=false (L52), or toEmail=null ---
    @Test
    void handle_emailEnabledFalse_doesNotSendEmailStillSaves() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-no-email")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .userId(3L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(false)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .whatsappEnabled(false)
                .build();

        when(processedEventRepository.existsById("evt-no-email")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verifyNoInteractions(emailSender, whatsAppSender);
        verify(processedEventRepository).save(any());
    }

    @Test
    void handle_toEmailNullWithEmailEnabled_doesNotSendEmailSendsWhatsAppIfEnabled() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-email-null")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .userId(3L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail(null)
                .whatsappEnabled(true)
                .whatsappPhoneOrApiKey("key")
                .defaultToPhone("+123")
                .build();

        when(processedEventRepository.existsById("evt-email-null")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verifyNoInteractions(emailSender);
        verify(whatsAppSender).send(any(WhatsAppRequest.class), any(TenantWhatsAppConfig.class));
        verify(processedEventRepository).save(any());
    }

    // --- Branches: whatsappEnabled=false (L63), toPhone=null, whatsappPhoneOrApiKey=null ---
    @Test
    void handle_whatsappEnabledFalse_doesNotSendWhatsApp() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-no-wa")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .userId(3L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .whatsappEnabled(false)
                .defaultToPhone("+123")
                .build();

        when(processedEventRepository.existsById("evt-no-wa")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verify(emailSender).send(any(EmailRequest.class), any(TenantMailConfig.class));
        verifyNoInteractions(whatsAppSender);
        verify(processedEventRepository).save(any());
    }

    @Test
    void handle_toPhoneNullWithWhatsAppEnabled_doesNotSendWhatsApp() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-wa-phone-null")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .whatsappEnabled(true)
                .whatsappPhoneOrApiKey("key")
                .defaultToPhone(null)
                .build();

        when(processedEventRepository.existsById("evt-wa-phone-null")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verify(emailSender).send(any(EmailRequest.class), any(TenantMailConfig.class));
        verifyNoInteractions(whatsAppSender);
        verify(processedEventRepository).save(any());
    }

    @Test
    void handle_whatsappPhoneOrApiKeyNull_doesNotSendWhatsApp() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-wa-key-null")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .whatsappEnabled(true)
                .whatsappPhoneOrApiKey(null)
                .defaultToPhone("+123")
                .build();

        when(processedEventRepository.existsById("evt-wa-key-null")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        verify(emailSender).send(any(EmailRequest.class), any(TenantMailConfig.class));
        verifyNoInteractions(whatsAppSender);
        verify(processedEventRepository).save(any());
    }

    // --- buildEventId: eventId not null but blank (L74); eventType null (L77); occurredAt null (L78) ---
    @Test
    void handle_eventIdBlank_usesFallbackEventId() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("   ")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .occurredAt(Instant.parse("2025-01-15T10:00:00Z"))
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<com.theratime.notification.entity.ProcessedEventEntity> saveCaptor =
                ArgumentCaptor.forClass(com.theratime.notification.entity.ProcessedEventEntity.class);
        verify(processedEventRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getEventId()).startsWith("1:10:appointment.created:");
        assertThat(saveCaptor.getValue().getEventId()).doesNotContain("   ");
    }

    @Test
    void handle_buildEventIdFallbackWithNullEventTypeAndOccurredAt() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId(null)
                .tenantId(2L)
                .appointmentId(20L)
                .therapistId(6L)
                .eventType(null)
                .occurredAt(null)
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(2L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(configResolver.resolve(2L, 6L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<com.theratime.notification.entity.ProcessedEventEntity> saveCaptor =
                ArgumentCaptor.forClass(com.theratime.notification.entity.ProcessedEventEntity.class);
        verify(processedEventRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getEventId()).isEqualTo("2:20::");
    }

    // --- buildSubject: eventType null (L83 switch gets "") -> default ---
    @Test
    void handle_eventTypeNull_buildSubjectReturnsDefault() {
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("evt-null-type")
                .tenantId(1L)
                .appointmentId(10L)
                .therapistId(5L)
                .userId(3L)
                .eventType(null)
                .occurredAt(Instant.now())
                .build();

        NotificationConfigEntity config = NotificationConfigEntity.builder()
                .tenantId(1L)
                .emailEnabled(true)
                .emailFrom("from@example.com")
                .smtpHost("smtp.example.com")
                .defaultToEmail("to@example.com")
                .build();

        when(processedEventRepository.existsById("evt-null-type")).thenReturn(false);
        when(configResolver.resolve(1L, 5L)).thenReturn(Optional.of(config));

        notificationHandler.handle(payload);

        ArgumentCaptor<EmailRequest> emailCaptor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailSender).send(emailCaptor.capture(), any(TenantMailConfig.class));
        assertThat(emailCaptor.getValue().getSubject()).isEqualTo("Appointment update");
    }
}

