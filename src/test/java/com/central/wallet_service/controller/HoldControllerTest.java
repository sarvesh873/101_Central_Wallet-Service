//package com.central.wallet_service.controller;
//
//import com.central.wallet_service.service.HoldService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.openapitools.model.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class HoldControllerTest {
//
//    @Mock
//    private HoldService holdService;
//
//    @InjectMocks
//    private HoldController holdController;
//
//    private HoldRequest testRequest;
//    private HoldResponse testResponse;
//
//    @BeforeEach
//    void setUp() {
//        // Setup test request
//        testRequest = new HoldRequest()
//                .userCode("USER123")
//                .amount(100.0)
//                .transactionId("TXN123");
//
//        // Setup test response
//        testResponse = new HoldResponse()
//                .holdId("HOLD123")
//                .userCode("USER123")
//                .originalAmount(100.0)
//                .remainingAmount(100.0)
//                .status(HoldResponse.StatusEnum.ACTIVE);
//    }
//
//    @Test
//    void placeHold_ValidRequest_ReturnsCreated() {
//        // Arrange
//        when(holdService.placeHold(any(HoldRequest.class))).thenReturn(testResponse);
//
//        // Act
//        ResponseEntity<HoldResponse> response = holdController.placeHold(testRequest);
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals("HOLD123", response.getBody().getHoldId());
//        assertEquals("ACTIVE", response.getBody().getStatus().getValue());
//        verify(holdService).placeHold(any(HoldRequest.class));
//    }
//
//    @Test
//    void getHold_ExistingHold_ReturnsOk() {
//        // Arrange
//        when(holdService.getHold("HOLD123")).thenReturn(testResponse);
//
//        // Act
//        ResponseEntity<HoldResponse> response = holdController.getHold("HOLD123");
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals("HOLD123", response.getBody().getHoldId());
//        verify(holdService).getHold("HOLD123");
//    }
//
//    @Test
//    void captureHold_ValidRequest_ReturnsOk() {
//        // Arrange
//        testResponse.setStatus(HoldResponse.StatusEnum.CAPTURED);
//        CaptureRequest captureRequest = new CaptureRequest().holdId("HOLD123");
//
//        when(holdService.captureHoldFunds(any(CaptureRequest.class))).thenReturn(testResponse);
//
//        // Act
//        ResponseEntity<HoldResponse> response = holdController.captureHoldFunds(captureRequest);
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals("CAPTURED", response.getBody().getStatus().getValue());
//        verify(holdService).captureHoldFunds(any(CaptureRequest.class));
//    }
//
//    @Test
//    void releaseHold_ValidRequest_ReturnsOk() {
//        // Arrange
//        testResponse.setStatus(HoldResponse.StatusEnum.RELEASED);
//        ReleaseHoldRequest releaseRequest = new ReleaseHoldRequest().reason("Test release");
//
//        when(holdService.releaseHold(anyString(), any(ReleaseHoldRequest.class))).thenReturn(testResponse);
//
//        // Act
//        ResponseEntity<HoldResponse> response = holdController.releaseHold("HOLD123", releaseRequest);
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals("RELEASED", response.getBody().getStatus().getValue());
//        verify(holdService).releaseHold(anyString(), any(ReleaseHoldRequest.class));
//    }
//}