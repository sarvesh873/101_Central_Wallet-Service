package com.central.wallet_service.dto.adapter.request;

import com.central.wallet.ReleaseHoldRequestGRPC;
import com.central.wallet_service.dto.ReleaseHoldRequestDto;

import java.util.Map;

public class GrpcReleaseHoldRequestAdapter implements ReleaseHoldRequestDto {

    private final ReleaseHoldRequestGRPC request;

    public GrpcReleaseHoldRequestAdapter(ReleaseHoldRequestGRPC request) {
        this.request = request;
    }

    @Override
    public String getReason() {
        return request.getReason();
    }

    @Override
    public Map<String, String> getMetadata() {
        return request.getMetadata();
    }
}
