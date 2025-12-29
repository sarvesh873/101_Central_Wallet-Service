package com.central.wallet_service.dto.adapter.response;

import com.central.wallet.WalletResponseGRPC;
import com.central.wallet_service.dto.WalletResponseDto;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class GrpcWalletResponseAdapter {
    
    private final WalletResponseDto walletResponseDto;
    
    public WalletResponseGRPC toGrpcResponse() {
        WalletResponseGRPC.Builder builder = WalletResponseGRPC.newBuilder()
                .setWalletId(walletResponseDto.getWalletId())
                .setUserCode(walletResponseDto.getUserCode())
                .setBalance(walletResponseDto.getBalance())
                .setAvailableBalance(walletResponseDto.getAvailableBalance())
                .setCurrency(walletResponseDto.getCurrency())
                .setStatus(walletResponseDto.getStatus())
                .setCreatedAt(convertToTimestamp(walletResponseDto.getCreatedAt()));

        // Set optional fields if they are not null
        if (walletResponseDto.getUsername() != null) {
            builder.setUsername(walletResponseDto.getUsername());
        }
        if (walletResponseDto.getEmail() != null) {
            builder.setEmail(walletResponseDto.getEmail());
        }
        if (walletResponseDto.getPhoneNumber() != null) {
            builder.setPhoneNumber(walletResponseDto.getPhoneNumber());
        }

        return builder.build();
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
