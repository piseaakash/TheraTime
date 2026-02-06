package com.theratime.appointment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theratime.appointment.entity.Appointment;
import com.theratime.appointment.entity.AppointmentStatus;
import com.theratime.appointment.entity.OutboxEntity;
import com.theratime.appointment.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void enqueueEvent_success_serializesPayloadAndSavesOutbox() throws Exception {
        Long tenantId = 1L;
        Appointment appointment = Appointment.builder()
                .id(10L)
                .userId(3L)
                .therapistId(2L)
                .status(AppointmentStatus.BOOKED)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"json\":\"payload\"}");

        outboxService.enqueueEvent(tenantId, appointment, "appointment.created");

        ArgumentCaptor<OutboxEntity> captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getEventType()).isEqualTo("appointment.created");
        assertThat(saved.getPayload()).isEqualTo("{\"json\":\"payload\"}");
        assertThat(saved.getStatus()).isEqualTo(OutboxEntity.STATUS_PENDING);
        assertThat(saved.getAttemptCount()).isEqualTo(0);
    }

    @Test
    void enqueueEvent_whenSerializationFails_doesNotSaveOutbox() throws Exception {
        Long tenantId = 1L;
        Appointment appointment = Appointment.builder()
                .id(10L)
                .userId(3L)
                .therapistId(2L)
                .build();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});

        outboxService.enqueueEvent(tenantId, appointment, "appointment.created");

        verify(outboxRepository, never()).save(any());
    }
}

