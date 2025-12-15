package com.central.wallet_service.dto;

import java.time.OffsetDateTime;

public interface HoldRequestDto {
    String getUserCode();
    Double getAmount();
    String getCurrency();
    OffsetDateTime getExpiresAt();
    String getTransaction_id();
    String getDescription();
}
