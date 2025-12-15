package com.central.wallet_service.dto.adapter.request;

import com.central.wallet_service.dto.ReleaseHoldRequestDto;
import org.openapitools.model.ReleaseHoldRequest;

import java.util.Map;

public class RestReleaseHoldRequestAdapter implements ReleaseHoldRequestDto {

    private final ReleaseHoldRequest request;

    public RestReleaseHoldRequestAdapter(ReleaseHoldRequest request) {
        this.request = request;
    }

    @Override
    public String getReason() {
        return request.getReason();
    }

    @Override
    public Map<String, String> getMetadata() {
        return Map.of();
    }


}
