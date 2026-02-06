package com.theratime.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theratime.notification.entity.ProcessedEventEntity;
import com.theratime.notification.event.AppointmentEventPayload;
import com.theratime.notification.repository.ProcessedEventRepository;
import com.theratime.notification.send.EmailRequest;
import com.theratime.notification.send.EmailSender;
import com.theratime.notification.send.TenantMailConfig;
import com.theratime.notification.send.WhatsAppSender;
import com.theratime.notification.service.NotificationHandler;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class NotificationFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("notificationdb")
            .withUsername("notification_user")
            .withPassword("notification_pass");

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
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private NotificationHandler notificationHandler;

    @AfterEach
    void cleanup() {
        processedEventRepository.deleteAll();
    }

    @TestConfiguration
    static class TestOverrides {

        @Bean
        @Primary
        EmailSender testEmailSender() {
            // No-op; we test notification flow, not actual SMTP delivery
            return (EmailRequest request, TenantMailConfig config) -> {
            };
        }

        @Bean
        @Primary
        WhatsAppSender testWhatsAppSender() {
            return (req, cfg) -> {
            };
        }
    }

    @Test
    void transientFailureIsRetriedAndEventEventuallyProcessed() throws Exception {
        String eventId = UUID.randomUUID().toString();

        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId(eventId)
                .tenantId(1L)
                .appointmentId(42L)
                .userId(3L)
                .therapistId(2L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .build();
        String json = objectMapper.writeValueAsString(payload);

        // First handler invocation throws, second succeeds
        AtomicInteger counter = new AtomicInteger();
        doAnswer(invocation -> {
            if (counter.getAndIncrement() == 0) {
                throw new RuntimeException("Simulated transient failure");
            }
            return invocation.callRealMethod();
        }).when(notificationHandler).handle(any(AppointmentEventPayload.class));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>("appointment.events", "1", json)).get();
        }

        // Wait until event is eventually marked processed
        long deadline = System.currentTimeMillis() + 30_000;
        Optional<ProcessedEventEntity> processed = Optional.empty();
        while (System.currentTimeMillis() < deadline && processed.isEmpty()) {
            Thread.sleep(1_000);
            processed = processedEventRepository.findById(eventId);
        }

        assertThat(processed).isPresent();
    }

    @Test
    void permanentFailureEndsUpInDeadLetterTopic() throws Exception {
        String eventId = UUID.randomUUID().toString();

        AppointmentEventPayload payload = AppointmentEventPayload.builder()
                .eventId(eventId)
                .tenantId(1L)
                .appointmentId(99L)
                .userId(3L)
                .therapistId(2L)
                .eventType(AppointmentEventPayload.EVENT_CREATED)
                .build();
        String json = objectMapper.writeValueAsString(payload);

        // Always fail handler so @RetryableTopic exhausts attempts and routes to DLT
        doThrow(new RuntimeException("Permanent failure"))
                .when(notificationHandler)
                .handle(any(AppointmentEventPayload.class));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>("appointment.events", "1", json)).get();
        }

        // Expect message on dead-letter topic
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps("notification-dlt-it"))) {
            consumer.subscribe(Collections.singletonList("appointment.events.dlq"));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(60));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.value()).contains(eventId);
        }

        // And it should not be marked as processed
        assertThat(processedEventRepository.findById(eventId)).isEmpty();
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    private Properties consumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}

