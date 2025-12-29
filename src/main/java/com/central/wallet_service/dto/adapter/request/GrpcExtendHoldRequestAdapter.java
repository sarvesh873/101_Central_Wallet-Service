package com.central.wallet_service.dto.adapter.request;

import com.central.wallet.ExtendHoldRequestGRPC;
import com.central.wallet_service.dto.ExtendHoldRequestDto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class GrpcExtendHoldRequestAdapter implements ExtendHoldRequestDto {

    private final ExtendHoldRequestGRPC request;

    public GrpcExtendHoldRequestAdapter(ExtendHoldRequestGRPC request) {
        this.request = request;
    }

    @Override
    public OffsetDateTime getNewExpiryTime() {
        return OffsetDateTime.ofInstant(
            Instant.ofEpochSecond(
                request.getNewExpiresAt().getSeconds(),
                request.getNewExpiresAt().getNanos()
            ),
            ZoneOffset.UTC
        );
    }

    @Override
    public String getReason() {
        return request.getReason();
    }

}
