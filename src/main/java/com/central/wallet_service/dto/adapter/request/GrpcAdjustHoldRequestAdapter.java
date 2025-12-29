package com.central.wallet_service.dto.adapter.request;

import com.central.wallet.AdjustHoldRequestGRPC;
import com.central.wallet_service.dto.AdjustHoldRequestDto;

import java.util.HashMap;
import java.util.Map;

public class GrpcAdjustHoldRequestAdapter implements AdjustHoldRequestDto {

    private final AdjustHoldRequestGRPC request;

    public GrpcAdjustHoldRequestAdapter(AdjustHoldRequestGRPC request) {
        this.request = request;
    }

    @Override
    public Double getNewAmount() {
        return request.getNewAmount();
    }

    @Override
    public String getReason() {
        return request.getReason();
    }
}
