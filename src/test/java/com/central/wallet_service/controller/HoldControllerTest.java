package com.central.wallet_service.controller;

import com.central.wallet_service.dto.CaptureRequestDto;
import com.central.wallet_service.dto.HoldResponseDto;
import com.central.wallet_service.dto.adapter.response.HoldResponseAdapter;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletHold;
import com.central.wallet_service.model.WalletUserSnapshot;
import com.central.wallet_service.service.HoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.openapitools.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Add any necessary imports for your specific DTOs and models

@ExtendWith(MockitoExtension.class)
class HoldControllerTest {

    @Mock
    private HoldService holdService;

    @InjectMocks
    private HoldController holdController;

    private HoldRequest testRequest;
    private HoldResponse testResponse;
    private HoldResponseDto testResponseDto;
    private WalletHold testWalletHold;

    @BeforeEach
    void setUp() {
        // Setup test request
        testRequest = new HoldRequest()
                .userCode("USER123")
                .amount(100.0)
                .transactionId("TXN123");

        // Setup test WalletHold
        testWalletHold = new WalletHold();
        testWalletHold.setHoldId("HOLD123");
        testWalletHold.setTransaction_id("TXN123");
        testWalletHold.setOriginalAmount(100.0);
        testWalletHold.setRemainingAmount(100.0);
        testWalletHold.setCapturedAmount(0.0);
        testWalletHold.setStatus(HoldStatus.ACTIVE);
        testWalletHold.setDescription("Test hold");
        testWalletHold.setCreatedAt(LocalDateTime.now());
        testWalletHold.setUpdatedAt(LocalDateTime.now());
        testWalletHold.setExpiresAt(LocalDateTime.now().plusDays(1));
        
        // Set up wallet with user snapshot
        Wallet wallet = new Wallet();
        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
        userSnapshot.setUserCode("USER123");
        wallet.setUserSnapshot(userSnapshot);
        testWalletHold.setWallet(wallet);

        // Create HoldResponseDto using adapter
        testResponseDto = new HoldResponseAdapter(testWalletHold);

        // Setup test response
        testResponse = new HoldResponse()
                .holdId("HOLD123")
                .userCode("USER123")
                .originalAmount(100.0)
                .remainingAmount(100.0)
                .status(HoldResponse.StatusEnum.ACTIVE);
    }

    @Test
    void placeHold_ValidRequest_ReturnsOk() {
        // Arrange
        when(holdService.placeHold(any())).thenReturn(testResponseDto);

        // Act
        ResponseEntity<HoldResponse> response = holdController.placeHold(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HOLD123", response.getBody().getHoldId());
        assertEquals(HoldResponse.StatusEnum.ACTIVE, response.getBody().getStatus());
        verify(holdService, times(1)).placeHold(any());
    }

    @Test
    void getHold_ExistingHold_ReturnsOk() {
        // Arrange
        when(holdService.getHold("HOLD123")).thenReturn(testResponseDto);

        // Act
        ResponseEntity<HoldResponse> response = holdController.getHold("HOLD123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HOLD123", response.getBody().getHoldId());
        verify(holdService).getHold("HOLD123");
    }

    @Test
    void captureHold_ValidRequest_ReturnsOk() {
        // Arrange
        testWalletHold.setStatus(HoldStatus.CAPTURED);
        testResponseDto = new HoldResponseAdapter(testWalletHold);
        CaptureRequest captureRequest = new CaptureRequest()
                .holdId("HOLD123");

        when(holdService.captureHoldFunds(any())).thenReturn(testResponseDto);

        // Act
        ResponseEntity<HoldResponse> response = holdController.captureHoldFunds(captureRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CAPTURED", response.getBody().getStatus().getValue());
        verify(holdService).captureHoldFunds(any(CaptureRequestDto.class));
    }

    @Test
    void releaseHold_ValidRequest_ReturnsOk() {
        // Arrange
        testWalletHold.setStatus(HoldStatus.RELEASED);
        testResponseDto = new HoldResponseAdapter(testWalletHold);

        ReleaseHoldRequest releaseRequest = new ReleaseHoldRequest()
                .reason("Test release");

        when(holdService.releaseHold(anyString(), any())).thenReturn(testResponseDto);

        // Act
        ResponseEntity<HoldResponse> response = holdController.releaseHold("HOLD123", releaseRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RELEASED", response.getBody().getStatus().getValue());
        verify(holdService).releaseHold(eq("HOLD123"), any());
    }

    @Test
    void adjustHold_ValidRequest_ReturnsOk() {
        // Arrange
        testWalletHold.setStatus(HoldStatus.ACTIVE);
        testWalletHold.setRemainingAmount(50.0);
        testResponseDto = new HoldResponseAdapter(testWalletHold);

        AdjustHoldRequest adjustRequest = new AdjustHoldRequest()
                .newAmount(150.0)
                .reason("Adjusting amount");

        when(holdService.adjustHold(eq("HOLD123"), any())).thenReturn(testResponseDto);

        // Act
        ResponseEntity<HoldResponse> response = holdController.adjustHold("HOLD123", adjustRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACTIVE", response.getBody().getStatus().getValue());
        verify(holdService).adjustHold(eq("HOLD123"), any());
    }

    @Test
    void extendHold_ValidRequest_ReturnsOk() {
        // Arrange
        testWalletHold.setStatus(HoldStatus.ACTIVE);
        testResponseDto = new HoldResponseAdapter(testWalletHold);

        ExtendHoldRequest extendRequest = new ExtendHoldRequest()
                .newExpiresAt(OffsetDateTime.now().plusDays(7))
                .reason("Extending hold period");

        when(holdService.extendHold(eq("HOLD123"), any())).thenReturn(testResponseDto);

        // Act
        ResponseEntity<HoldResponse> response = holdController.extendHold("HOLD123", extendRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACTIVE", response.getBody().getStatus().getValue());
        verify(holdService).extendHold(eq("HOLD123"), any());
    }

    @Test
    void listHolds_ValidRequest_ReturnsOk() {
        // Arrange
        Page<HoldResponseDto> page = new PageImpl<>(Collections.singletonList(testResponseDto));
        when(holdService.listHolds(
                eq("USER123"), 
                eq("ACTIVE"), 
                eq("USD"), 
                any(), 
                any(), 
                any(Pageable.class))).thenReturn(page);

        // Act
        ResponseEntity<ListHolds200Response> response = holdController.listHolds(
                "USER123", "ACTIVE", "USD", 
                OffsetDateTime.now().minusDays(7), 
                OffsetDateTime.now(), 
                0, 10, "createdAt", true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getItems().size());
        assertEquals("HOLD123", response.getBody().getItems().get(0).getHoldId());
        verify(holdService).listHolds(
                eq("USER123"), 
                eq("ACTIVE"), 
                eq("USD"), 
                any(), 
                any(), 
                any(Pageable.class));
    }
}