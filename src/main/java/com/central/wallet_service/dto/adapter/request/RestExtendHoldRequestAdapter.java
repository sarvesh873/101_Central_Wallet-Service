package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.ExtendHoldRequestDto;
import org.openapitools.model.ExtendHoldRequest;

import java.time.OffsetDateTime;
import java.util.Map;

public class RestExtendHoldRequestAdapter implements ExtendHoldRequestDto {

    private final ExtendHoldRequest request;

    public RestExtendHoldRequestAdapter(ExtendHoldRequest request) {
        this.request = request;
    }

    @Override
    public OffsetDateTime getNewExpiryTime() {
        return request.getNewExpiresAt();
    }

    @Override
    public String getReason() {
        return request.getReason();
    }
}
