package com.central.wallet_service.dto;

public interface WalletTransactionResponseDto {
    enum TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }
    
    enum TransactionStatus {
        COMPLETED,
        FAILED,
        DECLINED,
        EXPIRED
    }
    
    Long getWalletId();
    TransactionType getTransactionType();
    Double getProcessedAmount();
    Double getNewBalance();
    Double getNewAvailableBalance();
    String getUserCode();
    TransactionStatus getStatus();
    String getUsername();
    String getEmail();
    String getPhoneNumber();
}
