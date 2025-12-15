package com.central.wallet_service.dto.adapter.request;

import com.central.wallet.WalletTransactionRequestGRPC;
import com.central.wallet_service.dto.WalletTransactionRequestDto;

public class GrpcTransactionRequestAdapter implements WalletTransactionRequestDto {

    private final WalletTransactionRequestGRPC grpcRequest;

    public GrpcTransactionRequestAdapter(WalletTransactionRequestGRPC grpcRequest) {
        this.grpcRequest = grpcRequest;
    }

    @Override
    public Double getAmount() {
        return grpcRequest.getAmount();
    }

    @Override
    public String getCurrency() {
        return grpcRequest.getCurrency();
    }
}
