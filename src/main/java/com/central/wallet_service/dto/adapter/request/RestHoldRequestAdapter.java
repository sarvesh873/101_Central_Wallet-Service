package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.PlaceHoldRequestDto;
import org.openapitools.model.HoldRequest;

public class RestHoldRequestAdapter implements PlaceHoldRequestDto {

    private final HoldRequest openApiRequest;

    public RestHoldRequestAdapter(HoldRequest openApiRequest) {
        this.openApiRequest = openApiRequest;
    }

    @Override
    public String getUserCode() {
        return openApiRequest.getUserCode();
    }

    @Override
    public Double getAmount() {
        return openApiRequest.getAmount();
    }

    @Override
    public String getDescription() {
        return openApiRequest.getDescription();
    }

    @Override
    public String getTransactionId() {
        return openApiRequest.getTransactionId();
    }
}
