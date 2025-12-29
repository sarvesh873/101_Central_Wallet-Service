package com.central.wallet_service.dto.adapter.response;

import com.central.wallet_service.dto.HoldResponseDto;
import com.central.wallet_service.model.WalletHold;
import org.openapitools.model.HoldResponse;
import org.openapitools.model.HoldResponse.StatusEnum;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public class HoldResponseAdapter implements HoldResponseDto {
    private final WalletHold hold;

    public HoldResponseAdapter(WalletHold hold) {
        this.hold = hold;
    }

    @Override
    public String getHoldId() {
        return hold.getHoldId();
    }

    @Override
    public String getTransaction_id() {
        return hold.getTransaction_id();
    }

    @Override
    public String getUserCode() {
        return hold.getWallet() != null && hold.getWallet().getUserSnapshot() != null ?
                hold.getWallet().getUserSnapshot().getUserCode() : null;
    }

    @Override
    public String getStatus() {
        return hold.getStatus() != null ? hold.getStatus().name() : null;
    }

    @Override
    public Double getOriginalAmount() {
        return hold.getOriginalAmount();
    }

    @Override
    public Double getRemainingAmount() {
        return hold.getRemainingAmount();
    }

    @Override
    public Double getCapturedAmount() {
        return hold.getCapturedAmount();
    }

    @Override
    public String getDescription() {
        return hold.getDescription();
    }

    @Override
    public OffsetDateTime getCreatedAt() {
        return hold.getCreatedAt() != null ?
                hold.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }

    @Override
    public OffsetDateTime getUpdatedAt() {
        return hold.getUpdatedAt() != null ?
                hold.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }

    @Override
    public OffsetDateTime getExpiresAt() {
        return hold.getExpiresAt() != null ?
                hold.getExpiresAt().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}