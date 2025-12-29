package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.WalletCreateRequestDto;
import org.openapitools.model.WalletCreateRequest;

public class RestWalletRequestAdapter implements WalletCreateRequestDto {

    private final WalletCreateRequest openApiRequest;

    public RestWalletRequestAdapter(WalletCreateRequest openApiRequest) {
        this.openApiRequest = openApiRequest;
    }

    @Override
    public String getUserCode() {
        return openApiRequest.getUserCode();
    }

    @Override
    public String getCurrency() {
        return openApiRequest.getCurrency();
    }
}
