package com.theratime.notification.send;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantWhatsAppConfig {
    private String phoneOrApiKey;
}
