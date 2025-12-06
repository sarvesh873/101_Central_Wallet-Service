package com.central.wallet_service.controller;

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
        WalletTransactionResponse response = walletService.depositFunds(userCode, walletTransactionRequest);
        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<WalletTransactionResponse> withdrawFunds(String userCode,
                                                                   WalletTransactionRequest walletTransactionRequest) {
        WalletTransactionResponse response = walletService.withdrawFunds(userCode, walletTransactionRequest);
        return ResponseEntity.ok(response);
    }

}
