package com.central.wallet_service.dto.adapter.response;

import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.central.wallet_service.model.Wallet;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class WalletTransactionResponseAdapter implements WalletTransactionResponseDto {
    private final Wallet wallet;
    private final Double processedAmount;
    private final String transactionType;

    @Override
    public Long getWalletId() {
        return wallet != null ? wallet.getId() : null;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.valueOf(transactionType);
    }

    @Override
    public Double getProcessedAmount() {
        return processedAmount;
    }

    @Override
    public Double getNewBalance() {
        return wallet != null ? wallet.getBalance() : 0.0;
    }

    @Override
    public Double getNewAvailableBalance() {
        return wallet != null ? wallet.getAvailableBalance() : 0.0;
    }

    @Override
    public String getUserCode() {
        return wallet != null && wallet.getUserSnapshot() != null ?
                wallet.getUserSnapshot().getUserCode() : null;
    }

    @Override
    public TransactionStatus getStatus() {
        return TransactionStatus.COMPLETED; // Assuming all transactions are completed if we reach here
    }

    @Override
    public String getUsername() {
        return wallet != null && wallet.getUserSnapshot() != null ?
                wallet.getUserSnapshot().getUsername() : null;
    }

    @Override
    public String getEmail() {
        return wallet != null && wallet.getUserSnapshot() != null ?
                wallet.getUserSnapshot().getEmail() : null;
    }

    @Override
    public String getPhoneNumber() {
        return wallet != null && wallet.getUserSnapshot() != null ?
                wallet.getUserSnapshot().getPhoneNumber() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletTransactionResponseAdapter that = (WalletTransactionResponseAdapter) o;
        return Objects.equals(getWalletId(), that.getWalletId()) &&
                Objects.equals(transactionType, that.transactionType) &&
                Objects.equals(processedAmount, that.processedAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWalletId(), transactionType, processedAmount);
    }
}