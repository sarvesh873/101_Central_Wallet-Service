package com.central.wallet_service.controller;

import com.central.wallet_service.dto.*;
import com.central.wallet_service.dto.adapter.request.RestTransactionRequestAdapter;
import com.central.wallet_service.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.WalletTransactionsApi;
import org.openapitools.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class WalletTransactionController implements WalletTransactionsApi {

    @Autowired
    private WalletService walletService;

    @Override
    public ResponseEntity<WalletTransactionResponse> depositFunds(String userCode,
                                                                  WalletTransactionRequest walletTransactionRequest) {
        WalletTransactionRequestDto requestDto = new RestTransactionRequestAdapter(walletTransactionRequest);
        WalletTransactionResponseDto responseDto = walletService.depositFunds(userCode, requestDto);
        return ResponseEntity.ok(toWalletTransactionResponse(responseDto));
    }

    @Override
    public ResponseEntity<WalletTransactionResponse> withdrawFunds(String userCode,
                                                                   WalletTransactionRequest walletTransactionRequest) {
        WalletTransactionRequestDto requestDto = new RestTransactionRequestAdapter(walletTransactionRequest);
        WalletTransactionResponseDto responseDto = walletService.withdrawFunds(userCode, requestDto);
        return ResponseEntity.ok(toWalletTransactionResponse(responseDto));
    }
    
    private WalletTransactionResponse toWalletTransactionResponse(WalletTransactionResponseDto dto) {
        return new WalletTransactionResponse()
            .walletId(dto.getWalletId())
            .transactionType(WalletTransactionResponse.TransactionTypeEnum.fromValue(dto.getTransactionType().name()))
            .processedAmount(dto.getProcessedAmount())
            .newBalance(dto.getNewBalance())
            .newAvailableBalance(dto.getNewAvailableBalance())
            .userCode(dto.getUserCode())
            .status(WalletTransactionResponse.StatusEnum.fromValue(dto.getStatus().name()))
            .username(dto.getUsername())
            .email(dto.getEmail())
            .phoneNumber(dto.getPhoneNumber());
    }

}
