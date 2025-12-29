package com.central.wallet_service.dto;

import java.util.Map;

public interface ReleaseHoldRequestDto {
    String getReason();
    Map<String,String> getMetadata();
}
