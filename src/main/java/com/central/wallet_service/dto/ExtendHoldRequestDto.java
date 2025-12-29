package com.central.wallet_service.dto;

import java.time.OffsetDateTime;

public interface ExtendHoldRequestDto {
    OffsetDateTime getNewExpiryTime();
    String getReason();
}
