package com.central.wallet_service.dto;

public interface CaptureRequestDto {
    String getHoldId();
    String getTransaction_id();
    String getDescription();
    Boolean getReleaseRemainder();
}
