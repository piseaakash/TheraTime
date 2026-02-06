package com.theratime.notification.repository;

import com.theratime.notification.entity.NotificationConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationConfigRepository extends JpaRepository<NotificationConfigEntity, Long> {

    Optional<NotificationConfigEntity> findByTenantIdAndTherapistId(Long tenantId, Long therapistId);

    Optional<NotificationConfigEntity> findByTenantIdAndTherapistIdIsNull(Long tenantId);
}
