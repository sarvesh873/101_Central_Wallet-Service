package com.central.wallet_service.controller;

import com.central.wallet_service.dto.WalletCreateRequestDto;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.WalletCreateRequest;
import org.openapitools.model.WalletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private static final String TEST_USER_CODE = "USER123";
    private static final Long TEST_WALLET_ID = 1L;
    private static final String TEST_CURRENCY = "USD";
    private static final String TEST_STATUS = "ACTIVE";
    private static final Double TEST_BALANCE = 1000.00;
    private static final OffsetDateTime TEST_CREATED_AT = OffsetDateTime.now();
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PHONE = "+1234567890";

    private WalletResponseDto createTestWalletResponse() {
        WalletResponseDto response = mock(WalletResponseDto.class);
        when(response.getWalletId()).thenReturn(TEST_WALLET_ID);
        when(response.getUserCode()).thenReturn(TEST_USER_CODE);
        when(response.getBalance()).thenReturn(TEST_BALANCE);
        when(response.getAvailableBalance()).thenReturn(TEST_BALANCE);
        when(response.getCurrency()).thenReturn(TEST_CURRENCY);
        when(response.getStatus()).thenReturn(TEST_STATUS);
        when(response.getCreatedAt()).thenReturn(TEST_CREATED_AT);
        when(response.getUsername()).thenReturn(TEST_USERNAME);
        when(response.getEmail()).thenReturn(TEST_EMAIL);
        when(response.getPhoneNumber()).thenReturn(TEST_PHONE);
        return response;
    }

    private WalletCreateRequest createTestWalletRequest() {
        return new WalletCreateRequest()
                .userCode(TEST_USER_CODE)
                .currency(TEST_CURRENCY);
    }

    @Test
    void createWallet_ValidRequest_ReturnsOk() {
        // Arrange
        WalletResponseDto testResponse = createTestWalletResponse();
        when(walletService.createWallet(any(WalletCreateRequestDto.class)))
                .thenReturn(testResponse);

        // Act
        ResponseEntity<WalletResponse> response = walletController.createWallet(createTestWalletRequest());

        // Assert
        assertResponse(response, HttpStatus.OK);
        verify(walletService).createWallet(any(WalletCreateRequestDto.class));
    }

    @Test
    void getWalletByUserCode_ExistingUser_ReturnsOk() {
        // Arrange
        WalletResponseDto testResponse = createTestWalletResponse();
        when(walletService.getWalletByUserCode(TEST_USER_CODE))
                .thenReturn(testResponse);

        // Act
        ResponseEntity<WalletResponse> response = walletController.getWalletByUserCode(TEST_USER_CODE);

        // Assert
        assertResponse(response, HttpStatus.OK);
        verify(walletService).getWalletByUserCode(TEST_USER_CODE);
    }

    @Test
    void getWalletByUserCode_NonExistentUser_ThrowsException() {
        // Arrange
        when(walletService.getWalletByUserCode("NON_EXISTENT"))
                .thenThrow(new RuntimeException("Wallet not found"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> walletController.getWalletByUserCode("NON_EXISTENT"));
        verify(walletService).getWalletByUserCode("NON_EXISTENT");
    }

    private void assertResponse(ResponseEntity<WalletResponse> response, HttpStatus expectedStatus) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());

        WalletResponse body = response.getBody();
        assertEquals(TEST_WALLET_ID, body.getWalletId());
        assertEquals(TEST_USER_CODE, body.getUserCode());
        assertEquals(TEST_BALANCE, body.getBalance());
        assertEquals(TEST_BALANCE, body.getAvailableBalance());
        assertEquals(TEST_CURRENCY, body.getCurrency());
        assertEquals(TEST_STATUS, body.getStatus());
        assertEquals(TEST_CREATED_AT, body.getCreatedAt());
        assertEquals(TEST_USERNAME, body.getUsername());
        assertEquals(TEST_EMAIL, body.getEmail());
        assertEquals(TEST_PHONE, body.getPhoneNumber());
    }
}