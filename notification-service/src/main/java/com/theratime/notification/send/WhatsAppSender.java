package com.theratime.notification.send;

/**
 * Sends WhatsApp using tenant-scoped settings (phone or API key from config).
 */
public interface WhatsAppSender {

    void send(WhatsAppRequest request, TenantWhatsAppConfig config);
}
