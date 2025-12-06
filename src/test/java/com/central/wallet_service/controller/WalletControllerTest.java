package com.central.wallet_service.controller;

import com.central.wallet_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.WalletCreateRequest;
import org.openapitools.model.WalletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private WalletResponse testResponse;
    private WalletCreateRequest testRequest;

    @BeforeEach
    void setUp() {
        testResponse = new WalletResponse()
                .walletId(1L)
                .userCode("USER123")
                .balance(1000.0)
                .availableBalance(1000.0)
                .currency("USD")
                .status("ACTIVE");

        testRequest = new WalletCreateRequest()
                .userCode("USER123")
                .currency("USD");
    }

    @Test
    void createWallet_ValidRequest_ReturnsOk() {
        // Arrange
        when(walletService.createWallet(any(WalletCreateRequest.class)))
            .thenReturn(testResponse);

        // Act
        ResponseEntity<WalletResponse> response = walletController.createWallet(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getWalletId());
        assertEquals("ACTIVE", response.getBody().getStatus());
        verify(walletService).createWallet(any(WalletCreateRequest.class));
    }

    @Test
    void getWalletByUserCode_ExistingUser_ReturnsOk() {
        // Arrange
        when(walletService.getWalletByUserCode("USER123"))
            .thenReturn(testResponse);

        // Act
        ResponseEntity<WalletResponse> response = walletController.getWalletByUserCode("USER123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getWalletId());
        assertEquals(1000.0, response.getBody().getBalance());
        verify(walletService).getWalletByUserCode("USER123");
    }
}