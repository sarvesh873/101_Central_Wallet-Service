package com.central.wallet_service.dto.adapter.response;

import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.model.Wallet;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public class WalletResponseAdapter implements WalletResponseDto {
    private final Wallet wallet;

    public WalletResponseAdapter(Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    public Long getWalletId() {
        return wallet.getId();
    }

    @Override
    public String getUserCode() {
        return wallet.getUserSnapshot().getUserCode();
    }

    @Override
    public Double getBalance() {
        return wallet.getBalance();
    }

    @Override
    public String getStatus() {
        return wallet.getWalletStatus() != null ? wallet.getWalletStatus().name() : null;
    }

    @Override
    public String getCurrency() {
        return wallet.getCurrency();
    }

    @Override
    public Double getAvailableBalance() {
        return wallet.getAvailableBalance();
    }

    @Override
    public OffsetDateTime getCreatedAt() {
        return wallet.getCreatedAt() != null ?
            wallet.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }

    @Override
    public String getUsername() {
        return wallet.getUserSnapshot() != null ? wallet.getUserSnapshot().getUsername() : null;
    }

    @Override
    public String getEmail() {
        return wallet.getUserSnapshot() != null ? wallet.getUserSnapshot().getEmail() : null;
    }

    @Override
    public String getPhoneNumber() {
        return wallet.getUserSnapshot() != null ? wallet.getUserSnapshot().getPhoneNumber() : null;
    }
}
