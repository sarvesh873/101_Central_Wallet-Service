package com.central.wallet_service.controller;

import com.central.wallet_service.service.WalletService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.openapitools.model.*;


import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletTransactionControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletTransactionController walletTransactionController;

    private Validator validator;
    private WalletTransactionRequest testRequest;
    private WalletTransactionResponse testResponse;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        testRequest = new WalletTransactionRequest()
                .amount(100.0)
                .currency("USD");

        testResponse = new WalletTransactionResponse()
                .walletId(1L)
                .transactionType(WalletTransactionResponse.TransactionTypeEnum.DEPOSIT)
                .processedAmount(100.0)
                .newBalance(1100.0)
                .newAvailableBalance(1000.0)
                .userCode("USER123")
                .status(WalletTransactionResponse.StatusEnum.COMPLETED);
    }

    @Test
    void depositFunds_InvalidAmount_ValidatesNegativeAmount() {
        // Arrange
        WalletTransactionRequest invalidRequest = new WalletTransactionRequest()
                .amount(-100.0)
                .currency("USD");

        // Act
        Set<ConstraintViolation<WalletTransactionRequest>> violations =
                validator.validate(invalidRequest);

        // Assert
        assertFalse(violations.isEmpty(), "Expected validation to fail for negative amount");
    }

    @Test
    void withdrawFunds_InvalidAmount_ValidatesNegativeAmount() {
        // Arrange
        WalletTransactionRequest invalidRequest = new WalletTransactionRequest()
                .amount(-100.0)
                .currency("USD");

        // Act
        Set<ConstraintViolation<WalletTransactionRequest>> violations =
                validator.validate(invalidRequest);

        // Assert
        assertFalse(violations.isEmpty(), "Expected validation to fail for negative amount");
    }

    @Test
    void depositFunds_ValidRequest_ReturnsOk() {
        // Arrange
        when(walletService.depositFunds(anyString(), any(WalletTransactionRequest.class)))
                .thenReturn(testResponse);

        // Act
        ResponseEntity<WalletTransactionResponse> response =
                walletTransactionController.depositFunds("USER123", testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getWalletId());
        assertEquals("DEPOSIT", response.getBody().getTransactionType().getValue());
        assertEquals(100.0, response.getBody().getProcessedAmount());
        verify(walletService).depositFunds(anyString(), any(WalletTransactionRequest.class));
    }

    @Test
    void withdrawFunds_ValidRequest_ReturnsOk() {
        // Arrange
        testResponse.setTransactionType(WalletTransactionResponse.TransactionTypeEnum.WITHDRAWAL);
        when(walletService.withdrawFunds(anyString(), any(WalletTransactionRequest.class)))
                .thenReturn(testResponse);

        // Act
        ResponseEntity<WalletTransactionResponse> response =
                walletTransactionController.withdrawFunds("USER123", testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getWalletId());
        assertEquals("WITHDRAWAL", response.getBody().getTransactionType().getValue());
        verify(walletService).withdrawFunds(anyString(), any(WalletTransactionRequest.class));
    }
}

