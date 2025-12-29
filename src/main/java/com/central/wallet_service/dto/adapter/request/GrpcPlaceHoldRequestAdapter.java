package com.central.wallet_service.dto.adapter.request;

import com.central.wallet.PlaceHoldRequestGRPC;
import com.central.wallet_service.dto.PlaceHoldRequestDto;

public class GrpcPlaceHoldRequestAdapter implements PlaceHoldRequestDto {

    private final PlaceHoldRequestGRPC grpcRequest;

    public GrpcPlaceHoldRequestAdapter(PlaceHoldRequestGRPC grpcRequest) {
        this.grpcRequest = grpcRequest;
    }

    @Override
    public String getUserCode() {
        return grpcRequest.getUserCode();
    }

    @Override
    public Double getAmount() {
        return grpcRequest.getAmount();
    }

    @Override
    public String getDescription() {
        return grpcRequest.getDescription();
    }

    @Override
    public String getTransactionId() {
        return grpcRequest.getTransactionId();
    }
}
