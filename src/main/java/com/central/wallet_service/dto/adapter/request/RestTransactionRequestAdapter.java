package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.WalletTransactionRequestDto;
import org.openapitools.model.WalletTransactionRequest;

public class RestTransactionRequestAdapter implements WalletTransactionRequestDto {

    private final WalletTransactionRequest openApiRequest;

    public RestTransactionRequestAdapter(WalletTransactionRequest openApiRequest) {
        this.openApiRequest = openApiRequest;
    }

    @Override
    public Double getAmount() {
        return openApiRequest.getAmount();
    }

    @Override
    public String getCurrency() {
        return openApiRequest.getCurrency();
    }
}
