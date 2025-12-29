package com.central.wallet_service.dto.adapter.response;

import com.central.wallet.WalletTransactionResponseGRPC;
import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class GrpcTransactionResponseAdapter {
    
    private final WalletTransactionResponseDto responseDto;
    
    public WalletTransactionResponseGRPC toGrpcResponse() {
        return WalletTransactionResponseGRPC.newBuilder()
                .setWalletId(responseDto.getWalletId())
                .setTransactionType(WalletTransactionResponseGRPC.TransactionTypeGRPC.valueOf(responseDto.getTransactionType().name()))
                .setProcessedAmount(responseDto.getProcessedAmount())
                .setNewBalance(responseDto.getNewBalance())
                .setNewAvailableBalance(responseDto.getNewAvailableBalance())
                .setUserCode(responseDto.getUserCode())
                .setStatus(WalletTransactionResponseGRPC.TransactionStatusGRPC.valueOf(responseDto.getStatus().name()))
                .setUsername(responseDto.getUsername() != null ? responseDto.getUsername() : "")
                .setEmail(responseDto.getEmail() != null ? responseDto.getEmail() : "")
                .setPhoneNumber(responseDto.getPhoneNumber() != null ? responseDto.getPhoneNumber() : "")
                .build();
    }
    
    private static Timestamp convertToTimestamp(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        return Timestamp.newBuilder()
                .setSeconds(dateTime.toEpochSecond())
                .setNanos(dateTime.getNano())
                .build();
    }
}
