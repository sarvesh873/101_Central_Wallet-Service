package com.central.wallet_service.controller;

import com.central.wallet_service.dto.WalletCreateRequestDto;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.dto.adapter.request.RestWalletRequestAdapter;
import com.central.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.WalletsApi;
import org.openapitools.model.WalletCreateRequest;
import org.openapitools.model.WalletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WalletController implements WalletsApi {

    private final WalletService walletService;

    @Override
    public ResponseEntity<WalletResponse> createWallet(WalletCreateRequest walletCreateRequest) {
        WalletCreateRequestDto requestDto = new RestWalletRequestAdapter(walletCreateRequest);
        WalletResponseDto responseDto = walletService.createWallet(requestDto);
        return ResponseEntity.ok(toWalletResponse(responseDto));
    }

    @Override
    public ResponseEntity<WalletResponse> getWalletByUserCode(String userCode) {
        WalletResponseDto responseDto = walletService.getWalletByUserCode(userCode);
        return ResponseEntity.ok(toWalletResponse(responseDto));
    }
    
    private WalletResponse toWalletResponse(WalletResponseDto responseDto) {
        return new WalletResponse()
            .walletId(responseDto.getWalletId())
            .userCode(responseDto.getUserCode())
            .balance(responseDto.getBalance())
            .status(responseDto.getStatus())
            .currency(responseDto.getCurrency())
            .availableBalance(responseDto.getAvailableBalance())
            .createdAt(responseDto.getCreatedAt())
            .username(responseDto.getUsername())
            .email(responseDto.getEmail())
            .phoneNumber(responseDto.getPhoneNumber());
    }

}
