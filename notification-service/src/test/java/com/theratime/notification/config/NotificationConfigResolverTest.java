package com.theratime.notification.config;

import com.theratime.notification.entity.NotificationConfigEntity;
import com.theratime.notification.repository.NotificationConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConfigResolverTest {

    @Mock
    private NotificationConfigRepository repository;

    @InjectMocks
    private NotificationConfigResolver resolver;

    @Test
    void resolve_returnsTherapistSpecificConfigWhenPresent() {
        Long tenantId = 1L;
        Long therapistId = 5L;
        NotificationConfigEntity therapistConfig = NotificationConfigEntity.builder()
                .tenantId(tenantId)
                .therapistId(therapistId)
                .emailEnabled(true)
                .build();

        when(repository.findByTenantIdAndTherapistId(tenantId, therapistId))
                .thenReturn(Optional.of(therapistConfig));

        Optional<NotificationConfigEntity> result = resolver.resolve(tenantId, therapistId);

        assertThat(result).isPresent();
        assertThat(result.get().getTherapistId()).isEqualTo(therapistId);
        verify(repository).findByTenantIdAndTherapistId(tenantId, therapistId);
        verify(repository, never()).findByTenantIdAndTherapistIdIsNull(anyLong());
    }

    @Test
    void resolve_returnsTenantDefaultWhenTherapistSpecificNotFound() {
        Long tenantId = 1L;
        Long therapistId = 5L;
        NotificationConfigEntity tenantDefault = NotificationConfigEntity.builder()
                .tenantId(tenantId)
                .therapistId(null)
                .emailEnabled(true)
                .build();

        when(repository.findByTenantIdAndTherapistId(tenantId, therapistId))
                .thenReturn(Optional.empty());
        when(repository.findByTenantIdAndTherapistIdIsNull(tenantId))
                .thenReturn(Optional.of(tenantDefault));

        Optional<NotificationConfigEntity> result = resolver.resolve(tenantId, therapistId);

        assertThat(result).isPresent();
        assertThat(result.get().getTherapistId()).isNull();
        verify(repository).findByTenantIdAndTherapistId(tenantId, therapistId);
        verify(repository).findByTenantIdAndTherapistIdIsNull(tenantId);
    }

    @Test
    void resolve_whenTherapistIdNull_skipsTherapistLookupAndReturnsTenantDefault() {
        Long tenantId = 2L;
        NotificationConfigEntity tenantDefault = NotificationConfigEntity.builder()
                .tenantId(tenantId)
                .therapistId(null)
                .build();

        when(repository.findByTenantIdAndTherapistIdIsNull(tenantId))
                .thenReturn(Optional.of(tenantDefault));

        Optional<NotificationConfigEntity> result = resolver.resolve(tenantId, null);

        assertThat(result).isPresent();
        verify(repository, never()).findByTenantIdAndTherapistId(anyLong(), anyLong());
        verify(repository).findByTenantIdAndTherapistIdIsNull(tenantId);
    }

    @Test
    void resolve_returnsEmptyWhenNoConfigFound() {
        Long tenantId = 99L;
        Long therapistId = 10L;

        when(repository.findByTenantIdAndTherapistId(tenantId, therapistId))
                .thenReturn(Optional.empty());
        when(repository.findByTenantIdAndTherapistIdIsNull(tenantId))
                .thenReturn(Optional.empty());

        Optional<NotificationConfigEntity> result = resolver.resolve(tenantId, therapistId);

        assertThat(result).isEmpty();
    }
}
