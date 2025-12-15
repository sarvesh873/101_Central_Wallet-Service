package com.central.wallet_service.dto.adapter.request;

import com.central.wallet.WalletCreateRequestGRPC;
import com.central.wallet_service.dto.WalletCreateRequestDto;

public class GrpcWalletRequestAdapter implements WalletCreateRequestDto {

    private final WalletCreateRequestGRPC grpcRequest;

    public GrpcWalletRequestAdapter(WalletCreateRequestGRPC grpcRequest) {
        this.grpcRequest = grpcRequest;
    }

    @Override
    public String getUserCode() {
        return grpcRequest.getUserCode();
    }

    @Override
    public String getCurrency() {
        return grpcRequest.getCurrency();
    }
}
