package com.central.wallet_service.dto;

import java.time.OffsetDateTime;

public interface HoldResponseDto {
    String getHoldId();
    String getTransaction_id();
    String getUserCode();
    String getStatus();
    Double getOriginalAmount();
    Double getRemainingAmount();
    Double getCapturedAmount();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getExpiresAt();
    OffsetDateTime getUpdatedAt();
    String getDescription();
}
