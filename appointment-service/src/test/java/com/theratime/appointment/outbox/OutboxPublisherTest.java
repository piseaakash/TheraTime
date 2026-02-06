package com.theratime.appointment.outbox;

import com.theratime.appointment.entity.OutboxEntity;
import com.theratime.appointment.event.AppointmentEventPublisher;
import com.theratime.appointment.repository.OutboxRepository;
import com.theratime.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private AppointmentEventPublisher eventPublisher;

    @InjectMocks
    private OutboxPublisher publisher;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void publishPending_parsesTenantIdsAndPublishes() {
        ReflectionTestUtils.setField(publisher, "tenantIdsConfig", "1");
        ReflectionTestUtils.setField(publisher, "maxAttempts", 5);

        OutboxEntity row = OutboxEntity.builder()
                .id(10L)
                .tenantId(1L)
                .payload("{}")
                .status(OutboxEntity.STATUS_PENDING)
                .attemptCount(0)
                .build();
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEntity.STATUS_PENDING))
                .thenReturn(List.of(row));

        publisher.publishPending();

        verify(eventPublisher).sendPayload("1", "{}");
        ArgumentCaptor<OutboxEntity> captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository, atLeast(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEntity.STATUS_SENT);
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
    }

    @Test
    void publishPending_whenAttemptCountExceedsMax_marksFailed() {
        ReflectionTestUtils.setField(publisher, "tenantIdsConfig", "1");
        ReflectionTestUtils.setField(publisher, "maxAttempts", 3);

        OutboxEntity row = OutboxEntity.builder()
                .id(20L)
                .tenantId(1L)
                .payload("{}")
                .status(OutboxEntity.STATUS_PENDING)
                .attemptCount(3)
                .build();
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEntity.STATUS_PENDING))
                .thenReturn(List.of(row));

        publisher.publishPending();

        verify(eventPublisher, never()).sendPayload(anyString(), anyString());
        ArgumentCaptor<OutboxEntity> captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEntity.STATUS_FAILED);
    }

    @Test
    void publishPending_whenSendThrows_incrementsAttemptAndSaves() {
        ReflectionTestUtils.setField(publisher, "tenantIdsConfig", "1");
        ReflectionTestUtils.setField(publisher, "maxAttempts", 5);

        OutboxEntity row = OutboxEntity.builder()
                .id(30L)
                .tenantId(1L)
                .payload("{}")
                .status(OutboxEntity.STATUS_PENDING)
                .attemptCount(0)
                .build();
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEntity.STATUS_PENDING))
                .thenReturn(List.of(row));
        doThrow(new RuntimeException("kafka down")).when(eventPublisher).sendPayload(eq("1"), anyString());

        publisher.publishPending();

        ArgumentCaptor<OutboxEntity> captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEntity.STATUS_PENDING);
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
    }

    @Test
    void publishPending_whenAttemptCountNull_treatsAsZero() {
        ReflectionTestUtils.setField(publisher, "tenantIdsConfig", "1");
        ReflectionTestUtils.setField(publisher, "maxAttempts", 5);

        OutboxEntity row = OutboxEntity.builder()
                .id(40L)
                .tenantId(1L)
                .payload("{}")
                .status(OutboxEntity.STATUS_PENDING)
                .attemptCount(null)
                .build();
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEntity.STATUS_PENDING))
                .thenReturn(List.of(row));

        publisher.publishPending();

        verify(eventPublisher).sendPayload("1", "{}");
        ArgumentCaptor<OutboxEntity> captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository, atLeast(1)).save(captor.capture());
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
    }
}
