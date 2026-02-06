package com.theratime.notification.send;

/**
 * Sends email using tenant-scoped settings (from NotificationConfigEntity).
 */
public interface EmailSender {

    /**
     * Sends an email. Config holds SMTP and from address for the tenant/therapist.
     */
    void send(EmailRequest request, TenantMailConfig config);
}
