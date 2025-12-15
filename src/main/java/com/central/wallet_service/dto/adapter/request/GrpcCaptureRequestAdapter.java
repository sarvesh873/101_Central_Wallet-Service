package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.CaptureRequestDto;
import com.central.wallet.CaptureHoldRequestGRPC;

public class GrpcCaptureRequestAdapter implements CaptureRequestDto {

    private final CaptureHoldRequestGRPC request;

    public GrpcCaptureRequestAdapter(CaptureHoldRequestGRPC request) {
        this.request = request;
    }

    @Override
    public String getHoldId() {
        return request.getHoldId();
    }

    @Override
    public String getTransaction_id() {
        return request.getTransactionId();
    }

    @Override
    public String getDescription() {
        return request.getDescription();
    }

    @Override
    public Boolean getReleaseRemainder() {
        return request.getReleaseRemainder();
    }
}
