package com.theratime.appointment.outbox;

import com.theratime.appointment.entity.OutboxEntity;
import com.theratime.appointment.event.AppointmentEventPublisher;
import com.theratime.appointment.repository.OutboxRepository;
import com.theratime.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Polls outbox tables per tenant and publishes PENDING events to Kafka.
 * Retries with backoff: increments attempt_count and last_attempt_at on failure; marks FAILED after max attempts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final AppointmentEventPublisher eventPublisher;

    @Value("${app.outbox.tenant-ids:1,2}")
    private String tenantIdsConfig;

    @Value("${app.outbox.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms:5000}")
    public void publishPending() {
        List<Long> tenantIds = Arrays.stream(tenantIdsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        for (Long tenantId : tenantIds) {
            try {
                TenantContext.setTenantId(tenantId);
                List<OutboxEntity> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEntity.STATUS_PENDING);
                for (OutboxEntity row : pending) {
                    if (row.getAttemptCount() != null && row.getAttemptCount() >= maxAttempts) {
                        row.setStatus(OutboxEntity.STATUS_FAILED);
                        outboxRepository.save(row);
                        log.warn("Outbox id {} exceeded max attempts, marked FAILED", row.getId());
                        continue;
                    }
                    try {
                        String key = String.valueOf(row.getTenantId());
                        eventPublisher.sendPayload(key, row.getPayload());
                        row.setStatus(OutboxEntity.STATUS_SENT);
                        row.setLastAttemptAt(LocalDateTime.now());
                        row.setAttemptCount((row.getAttemptCount() != null ? row.getAttemptCount() : 0) + 1);
                        outboxRepository.save(row);
                        log.debug("Published outbox id {} to Kafka", row.getId());
                    } catch (Exception e) {
                        log.warn("Failed to publish outbox id {}: {}", row.getId(), e.getMessage());
                        row.setLastAttemptAt(LocalDateTime.now());
                        row.setAttemptCount((row.getAttemptCount() != null ? row.getAttemptCount() : 0) + 1);
                        outboxRepository.save(row);
                    }
                }
            } finally {
                TenantContext.clear();
            }
        }
    }
}
