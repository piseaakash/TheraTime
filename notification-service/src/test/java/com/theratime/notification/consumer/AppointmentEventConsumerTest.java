package com.theratime.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theratime.notification.event.AppointmentEventPayload;
import com.theratime.notification.service.NotificationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentEventConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationHandler notificationHandler;

    @InjectMocks
    private AppointmentEventConsumer consumer;

    @Test
    void consume_validJson_callsHandlerWithPayload() throws Exception {
        String message = "{\"eventId\":\"e1\",\"tenantId\":1,\"appointmentId\":10,\"eventType\":\"appointment.created\"}";
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId("e1")
                .tenantId(1L)
                .appointmentId(10L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .build();

        when(objectMapper.readValue(message, AppointmentEventPayload.class)).thenReturn(payload);

        consumer.consume(message);

        ArgumentCaptor<AppointmentEventPayload> captor = ArgumentCaptor.forClass(AppointmentEventPayload.class);
        verify(notificationHandler).handle(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getEventId()).isEqualTo("e1");
    }

    @Test
    void consume_nullTenantId_doesNotCallHandler() throws Exception {
        String message = "{\"tenantId\":null}";
        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .tenantId(null)
                .build();

        when(objectMapper.readValue(message, AppointmentEventPayload.class)).thenReturn(payload);

        consumer.consume(message);

        verifyNoInteractions(notificationHandler);
    }

    @Test
    void dltHandler_doesNotThrow() {
        consumer.dltHandler(
                "{\"payload\":\"dlq\"}",
                "Test exception",
                "stacktrace line"
        );
        // no exception = pass
    }
}
