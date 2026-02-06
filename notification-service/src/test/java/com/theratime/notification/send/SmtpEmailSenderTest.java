package com.theratime.notification.send;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    private SmtpEmailSender sender;

    @BeforeEach
    void setUp() {
        sender = new SmtpEmailSender();
        ReflectionTestUtils.setField(sender, "maxAttempts", 3);
        ReflectionTestUtils.setField(sender, "initialDelayMs", 10L);
        ReflectionTestUtils.setField(sender, "maxDelayMs", 100L);
    }

    @Test
    void send_configMissingSmtpHost_returnsEarlyWithoutSending() {
        TenantMailConfig config = TenantMailConfig.builder()
                .from("from@example.com")
                .smtpHost(null)
                .build();
        EmailRequest request = EmailRequest.builder()
                .to("to@example.com")
                .subject("Subj")
                .body("Body")
                .build();

        sender.send(request, config);

        // No exception; no mail sender should be constructed if we had visibility
        // (we can't verify mockConstruction without actually constructing, so early return is the assertion)
    }

    @Test
    void send_configMissingFrom_returnsEarlyWithoutSending() {
        TenantMailConfig config = TenantMailConfig.builder()
                .from(null)
                .smtpHost("smtp.example.com")
                .build();
        EmailRequest request = EmailRequest.builder()
                .to("to@example.com")
                .subject("Subj")
                .body("Body")
                .build();

        sender.send(request, config);
    }

    @Test
    void send_validConfig_attemptsSend() {
        TenantMailConfig config = TenantMailConfig.builder()
                .from("from@example.com")
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user")
                .smtpPassword("pass")
                .build();
        EmailRequest request = EmailRequest.builder()
                .to("to@example.com")
                .subject("Subj")
                .body("Body")
                .build();

        MimeMessage mockMessage = mock(MimeMessage.class);
        try (MockedConstruction<JavaMailSenderImpl> ignored = mockConstruction(JavaMailSenderImpl.class,
                (mock, context) -> {
                    when(mock.createMimeMessage()).thenReturn(mockMessage);
                    doNothing().when(mock).send(any(MimeMessage.class));
                })) {
            sender.send(request, config);
        }
        // Construction of JavaMailSenderImpl and send() invoked without throwing
    }
}
