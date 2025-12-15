package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.AdjustHoldRequestDto;
import org.openapitools.model.AdjustHoldRequest;

import java.util.Map;

public class RestAdjustHoldRequestAdapter implements AdjustHoldRequestDto {

    private final AdjustHoldRequest request;

    public RestAdjustHoldRequestAdapter(AdjustHoldRequest request) {
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
