package com.central.wallet_service.dto;

import java.time.OffsetDateTime;

public interface WalletResponseDto {
    Long getWalletId();
    String getUserCode();
    Double getBalance();
    String getStatus();
    String getCurrency();
    Double getAvailableBalance();
    OffsetDateTime getCreatedAt();
    String getUsername();
    String getEmail();
    String getPhoneNumber();
}
