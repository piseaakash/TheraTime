package com.theratime.notification.send;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub implementation: logs the message. Wire a real WhatsApp client (e.g. Twilio, WhatsApp Business API)
 * by implementing WhatsAppSender and excluding this bean when not using stub.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.whatsapp.stub", havingValue = "true", matchIfMissing = true)
public class StubWhatsAppSender implements WhatsAppSender {

    @Override
    public void send(WhatsAppRequest request, TenantWhatsAppConfig config) {
        log.info("[WhatsApp stub] tenant config key/phone={}, to={}, message={}",
                config.getPhoneOrApiKey(), request.getToPhone(), request.getMessage());
    }
}
