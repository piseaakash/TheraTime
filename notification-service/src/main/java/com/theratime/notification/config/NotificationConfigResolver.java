package com.theratime.notification.config;

import com.theratime.notification.entity.NotificationConfigEntity;
import com.theratime.notification.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolves notification config: per-therapist first, then tenant default.
 * (tenant_id, therapist_id) -> row; else (tenant_id, null) -> tenant default.
 */
@Service
@RequiredArgsConstructor
public class NotificationConfigResolver {

    private final NotificationConfigRepository repository;

    /**
     * Returns config for tenant and optional therapist.
     * Lookup order: (tenantId, therapistId), then (tenantId, null).
     */
    public Optional<NotificationConfigEntity> resolve(Long tenantId, Long therapistId) {
        if (therapistId != null) {
            Optional<NotificationConfigEntity> byTherapist = repository.findByTenantIdAndTherapistId(tenantId, therapistId);
            if (byTherapist.isPresent()) {
                return byTherapist;
            }
        }
        return repository.findByTenantIdAndTherapistIdIsNull(tenantId);
    }
}
