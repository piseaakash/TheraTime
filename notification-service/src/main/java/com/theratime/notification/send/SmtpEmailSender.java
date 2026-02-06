package com.theratime.notification.send;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmtpEmailSender implements EmailSender {

    @Value("${app.email.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.email.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${app.email.retry.max-delay-ms:10000}")
    private long maxDelayMs;

    @Override
    public void send(EmailRequest request, TenantMailConfig config) {
        if (config.getSmtpHost() == null || config.getFrom() == null) {
            log.warn("Email config incomplete: missing smtpHost or from");
            return;
        }

        long delayMillis = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(config.getSmtpHost());
                sender.setPort(config.getSmtpPort() != null ? config.getSmtpPort() : 25);
                if (config.getSmtpUsername() != null) {
                    sender.setUsername(config.getSmtpUsername());
                    sender.setPassword(config.getSmtpPassword());
                }
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setFrom(config.getFrom());
                helper.setTo(request.getTo());
                helper.setSubject(request.getSubject());
                helper.setText(request.getBody(), true);
                sender.send(message);
                log.info("Email sent to {} (tenant config) on attempt {}", request.getTo(), attempt);
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    log.warn("Failed to send email to {} after {} attempts: {}", request.getTo(), attempt, e.getMessage());
                } else {
                    log.warn("Failed to send email to {} on attempt {}: {}. Retrying...", request.getTo(), attempt, e.getMessage());
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Email retry interrupted");
                        return;
                    }
                    delayMillis = Math.min(delayMillis * 2, maxDelayMs);
                }
            }
        }
    }
}
