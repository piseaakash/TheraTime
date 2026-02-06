package com.theratime.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "therapist_id")
    private Long therapistId;

    @Column(name = "email_enabled")
    private Boolean emailEnabled;

    @Column(name = "email_from")
    private String emailFrom;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password_encrypted")
    private String smtpPasswordEncrypted;

    @Column(name = "whatsapp_enabled")
    private Boolean whatsappEnabled;

    @Column(name = "whatsapp_phone_or_api_key")
    private String whatsappPhoneOrApiKey;

    @Column(name = "default_to_email")
    private String defaultToEmail;

    @Column(name = "default_to_phone")
    private String defaultToPhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
