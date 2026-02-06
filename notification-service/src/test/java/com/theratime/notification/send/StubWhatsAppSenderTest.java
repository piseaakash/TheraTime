package com.theratime.notification.send;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class StubWhatsAppSenderTest {

    @InjectMocks
    private StubWhatsAppSender stubWhatsAppSender;

    @Test
    void send_doesNotThrow() {
        TenantWhatsAppConfig config = TenantWhatsAppConfig.builder()
                .phoneOrApiKey("stub-key")
                .build();
        WhatsAppRequest request = WhatsAppRequest.builder()
                .toPhone("+1234567890")
                .message("Hello")
                .build();

        assertThatCode(() -> stubWhatsAppSender.send(request, config))
                .doesNotThrowAnyException();
    }
}
