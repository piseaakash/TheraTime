package com.theratime.appointment.integration;

import com.theratime.appointment.entity.OutboxEntity;
import com.theratime.appointment.repository.OutboxRepository;
import com.theratime.appointment.service.AppointmentsService;
import com.theratime.appointment.service.OutboxService;
import com.theratime.appointment.service.UserService;
import com.theratime.appointments.model.BookAppointmentRequest;
import com.theratime.security.TenantContext;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AppointmentBookingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("appointmentsdb")
            .withUsername("appointment_user")
            .withPassword("appointment_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:7.5.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private AppointmentsService appointmentsService;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockBean
    private UserService userService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        outboxRepository.deleteAll();
    }

    @Test
    void bookAppointment_persistsOutboxAndPublishesKafkaEvent() {
        // Arrange authenticated user and tenant context
        var context = SecurityContextHolder.createEmptyContext();
        var auth = new TestingAuthenticationToken("user@example.com", null);
        auth.setAuthenticated(true);
        auth.setDetails(10L);
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        TenantContext.setTenantId(1L);

        when(userService.getTenantId(anyLong())).thenReturn(1L);
        when(userService.isUserPresent(anyLong())).thenReturn(true);
        when(userService.getUserRole(anyLong())).thenReturn("THERAPIST");

        BookAppointmentRequest request = new BookAppointmentRequest()
                .therapistId(2L)
                .userId(3L)
                .startTime(LocalDateTime.now().plusDays(1).atOffset(java.time.ZoneOffset.UTC))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1).atOffset(java.time.ZoneOffset.UTC));

        // Act: book appointment (writes appointment + outbox in same transaction)
        appointmentsService.bookAppointment(request);

        List<OutboxEntity> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEntity.STATUS_PENDING);
        assertThat(pending).hasSize(1);

        // Act: publish pending outbox entries to Kafka
        // Note: OutboxPublisher is already tested at unit level; here we verify that
        // messages actually arrive on a real Kafka broker.
        var publisher = new com.theratime.appointment.outbox.OutboxPublisher(outboxRepository,
                new com.theratime.appointment.event.AppointmentEventPublisher(
                        new org.springframework.kafka.core.KafkaTemplate<>(
                                new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(
                                        kafkaProducerProps()
                                )
                        ),
                        new com.fasterxml.jackson.databind.ObjectMapper()
                ));
        publisher.publishPending();

        // Assert: consume message from Kafka topic
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaConsumerProps())) {
            consumer.subscribe(Collections.singletonList("appointment.events"));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.key()).isEqualTo("1"); // tenant id as key
            assertThat(record.value()).contains("\"tenantId\":1");
        }
    }

    private Properties kafkaProducerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return props;
    }

    private Properties kafkaConsumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "appointment-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}

