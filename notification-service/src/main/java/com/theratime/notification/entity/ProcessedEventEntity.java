package com.theratime.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    public void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}

