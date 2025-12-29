package com.central.wallet_service.dto;

public interface PlaceHoldRequestDto {
    String getUserCode();
    Double getAmount();
    String getDescription();
    String getTransactionId();
}
