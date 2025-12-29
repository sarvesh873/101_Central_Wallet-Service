package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.CaptureRequestDto;
import org.openapitools.model.CaptureRequest;

public class RestCaptureRequestAdapter implements CaptureRequestDto {

    private final CaptureRequest request;

    public RestCaptureRequestAdapter(CaptureRequest request) {
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
