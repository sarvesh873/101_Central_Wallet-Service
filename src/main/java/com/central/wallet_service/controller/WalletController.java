package com.central.wallet_service.controller;

import com.central.wallet_service.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.WalletsApi;
import org.openapitools.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestController
public class WalletController implements WalletsApi{

    @Autowired
    private WalletService walletService;

    @Override
    public ResponseEntity<WalletResponse> createWallet(WalletCreateRequest walletCreateRequest) {
        WalletResponse response = walletService.createWallet(walletCreateRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<WalletResponse> getWalletByUserCode(String userCode) {
        WalletResponse response = walletService.getWalletByUserCode(userCode);
        return ResponseEntity.ok(response);
    }

}
