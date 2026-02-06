package com.theratime.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theratime.notification.event.AppointmentEventPayload;
import com.theratime.notification.service.NotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationHandler notificationHandler;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000L,
                    multiplier = 2.0,
                    maxDelay = 60000L
            ),
            dltTopicSuffix = ".dlq"
    )
    @KafkaListener(topics = "${app.kafka.topic.appointment-events:appointment.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) throws Exception {
        AppointmentEventPayload payload = objectMapper.readValue(message, AppointmentEventPayload.class);
        if (payload.getTenantId() == null) {
            log.warn("Ignoring event with missing tenantId");
            return;
        }
        notificationHandler.handle(payload);
    }

    @DltHandler
    public void dltHandler(
            String message,
            @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage,
            @Header(value = KafkaHeaders.DLT_EXCEPTION_STACKTRACE, required = false) String exceptionStacktrace
    ) {
        log.error(
                "Message sent to dead-letter topic for appointment events. Exception: {}, stacktrace: {}, payload: {}",
                exceptionMessage,
                exceptionStacktrace,
                message
        );
    }
}
