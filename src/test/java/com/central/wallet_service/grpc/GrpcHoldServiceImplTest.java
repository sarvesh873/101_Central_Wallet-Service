package com.central.wallet_service.grpc;

import com.central.wallet.*;
import com.central.wallet_service.dto.*;
import com.central.wallet_service.dto.adapter.request.*;
import com.central.wallet_service.dto.adapter.response.HoldResponseAdapter;
import com.central.wallet_service.exception.HoldNotFoundException;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletHold;
import com.central.wallet_service.model.WalletUserSnapshot;
import com.central.wallet_service.service.HoldService;
import com.central.wallet_service.utils.ServiceUtils;
import com.google.protobuf.Timestamp;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrpcHoldServiceImplTest {

    @Mock
    private HoldService holdService;

    @InjectMocks
    private GrpcHoldServiceImpl grpcHoldService;

    @Mock
    private StreamObserver<HoldResponseGRPC> responseObserver;
    
    @Mock
    private StreamObserver<ListHoldsResponseGRPC> listHoldsResponseObserver;

    private static final String TEST_HOLD_ID = "HOLD123";
    private static final String TEST_USER_CODE = "USER123";
    private static final String TEST_TXN_ID = "TXN123";
    private static final double TEST_AMOUNT = 100.0;
    private static final String TEST_DESCRIPTION = "Test hold";
    private static final String TEST_REASON = "Test reason";
    private static final String TEST_CURRENCY = "USD";
    private static final String TEST_STATUS = "ACTIVE";
    private static final long TEST_TIMESTAMP = System.currentTimeMillis();
    private static final Timestamp TEST_GRPC_TIMESTAMP = Timestamp.newBuilder()
        .setSeconds(TEST_TIMESTAMP / 1000)
        .setNanos((int) ((TEST_TIMESTAMP % 1000) * 1000000))
        .build();
    
    private static final OffsetDateTime TEST_OFFSET_DATETIME = OffsetDateTime.now(ZoneOffset.UTC);
    private static final com.google.protobuf.Timestamp TEST_GRPC_TIMESTAMP_NOW = 
        com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(TEST_OFFSET_DATETIME.toEpochSecond())
            .setNanos(TEST_OFFSET_DATETIME.getNano())
            .build();

    private HoldResponseDto createTestHoldResponseDto(HoldResponse.StatusEnum status) {
        // Create UserSnapshot
        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
        userSnapshot.setUserCode(TEST_USER_CODE);
        userSnapshot.setEmail("test@example.com");
        userSnapshot.setUsername("testuser");
        
        // Create Wallet
        Wallet wallet = new Wallet();
        wallet.setUserSnapshot(userSnapshot);
        wallet.setCurrency("USD");
        wallet.setBalance(1000.0);
        wallet.setAvailableBalance(1000.0);
        
        // Create WalletHold
        WalletHold walletHold = new WalletHold();
        walletHold.setHoldId(TEST_HOLD_ID);
        walletHold.setWallet(wallet);  // Set the wallet relationship
        walletHold.setOriginalAmount(TEST_AMOUNT);
        walletHold.setRemainingAmount(TEST_AMOUNT);
        walletHold.setCapturedAmount(0.0);
        walletHold.setStatus(status != null ? HoldStatus.valueOf(status.name()) : null);
        walletHold.setTransaction_id(TEST_TXN_ID);
        walletHold.setDescription(TEST_DESCRIPTION);
        walletHold.setCreatedAt(LocalDateTime.now());
        walletHold.setUpdatedAt(LocalDateTime.now());
        walletHold.setExpiresAt(LocalDateTime.now().plusDays(1));

        // Create and return the DTO using the adapter
        return new HoldResponseAdapter(walletHold);
    }
    
    private void verifyErrorResponse(Code expectedCode, String expectedMessage) {
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver, atLeastOnce()).onError(throwableCaptor.capture());
        
        // Get the last error
        Throwable throwable = throwableCaptor.getAllValues().get(throwableCaptor.getAllValues().size() - 1);
        assertTrue(throwable instanceof StatusRuntimeException);
        
        StatusRuntimeException statusException = (StatusRuntimeException) throwable;
        com.google.rpc.Status status = StatusProto.fromThrowable(statusException);
        
        assertEquals(expectedCode.getNumber(), status.getCode());
        assertTrue(status.getMessage().contains(expectedMessage),
            "Expected error message to contain: " + expectedMessage + ", but was: " + status.getMessage());
    }

    @Test
    void placeHold_ValidRequest_ReturnsSuccess() {
        // Arrange
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(TEST_AMOUNT)
                .setTransactionId("TEST_TXN_ID")
                .setDescription(TEST_DESCRIPTION)
                .setCurrency(TEST_CURRENCY)
                .build();

        // Create a fully populated HoldResponseDto
        HoldResponseDto holdResponse = createTestHoldResponseDto(HoldResponse.StatusEnum.ACTIVE);

        when(holdService.placeHold(any(PlaceHoldRequestDto.class))).thenReturn(holdResponse);

        // Act
        grpcHoldService.placeHold(request, responseObserver);

        // Assert
        ArgumentCaptor<PlaceHoldRequestDto> requestCaptor = ArgumentCaptor.forClass(PlaceHoldRequestDto.class);
        verify(holdService).placeHold(requestCaptor.capture());

        PlaceHoldRequestDto capturedRequest = requestCaptor.getValue();
        assertEquals(TEST_USER_CODE, capturedRequest.getUserCode());
        assertEquals(TEST_AMOUNT, capturedRequest.getAmount(), 0.001);
        assertEquals("TEST_TXN_ID", capturedRequest.getTransactionId());
        assertEquals(TEST_DESCRIPTION, capturedRequest.getDescription());

        ArgumentCaptor<HoldResponseGRPC> responseCaptor = ArgumentCaptor.forClass(HoldResponseGRPC.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        HoldResponseGRPC response = responseCaptor.getValue();
        assertEquals(TEST_HOLD_ID, response.getHoldId());
        assertEquals(TEST_USER_CODE, response.getUserCode());
        assertEquals(TEST_AMOUNT, response.getOriginalAmount(), 0.001);
        assertEquals(TEST_AMOUNT, response.getRemainingAmount(), 0.001);
        assertEquals(0.0, response.getCapturedAmount(), 0.001);
        assertEquals(TEST_DESCRIPTION, response.getDescription());
        assertEquals(HoldResponseGRPC.HoldStatusGRPC.ACTIVE, response.getStatus());
    }

    @Test
    void placeHold_InvalidRequest_MissingUserCode_ReturnsError() {
        // Test with empty user code
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode("")
                .setAmount(TEST_AMOUNT)
                .build();

        grpcHoldService.placeHold(request, responseObserver);
        verify(holdService, never()).placeHold(any());
        verifyErrorResponse(Code.INVALID_ARGUMENT, "Invalid request parameters");
        
        // Test with null user code
        request = PlaceHoldRequestGRPC.newBuilder()
                .setAmount(TEST_AMOUNT)
                .build();
                
        grpcHoldService.placeHold(request, responseObserver);
        verify(holdService, never()).placeHold(any());
        verifyErrorResponse(Code.INVALID_ARGUMENT, "Invalid request parameters");
    }

    @Test
    void placeHold_InvalidRequest_InvalidAmount_ReturnsError() {
        // Test with zero amount
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(0)
                .build();

        grpcHoldService.placeHold(request, responseObserver);
        verify(holdService, never()).placeHold(any());
        verifyErrorResponse(Code.INVALID_ARGUMENT, "Amount must be greater than zero");
        
        // Test with negative amount
        request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(-100)
                .build();

        grpcHoldService.placeHold(request, responseObserver);
        verify(holdService, never()).placeHold(any());
        verifyErrorResponse(Code.INVALID_ARGUMENT, "Amount must be greater than zero");
    }

    @Test
    void placeHold_DuplicateTransaction_ReturnsError() {
        // Arrange
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(TEST_AMOUNT)
                .setTransactionId(TEST_TXN_ID)
                .build();

        when(holdService.placeHold(any())).thenThrow(new DataIntegrityViolationException("Duplicate transaction"));

        // Act
        grpcHoldService.placeHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.ALREADY_EXISTS, "Duplicate transaction");
    }

    @Test
    void placeHold_WalletNotFound_ReturnsError() {
        // Arrange
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode("NON_EXISTENT")
                .setAmount(TEST_AMOUNT)
                .build();

        when(holdService.placeHold(any())).thenThrow(new WalletNotFoundException("Wallet not found"));

        // Act
        grpcHoldService.placeHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.NOT_FOUND, "Wallet not found");
    }

    @Test
    void placeHold_ServiceError_ReturnsInternalError() {
        // Arrange
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(TEST_AMOUNT)
                .build();

        when(holdService.placeHold(any())).thenThrow(new RuntimeException("Service error"));

        // Act
        grpcHoldService.placeHold(request, responseObserver);

    }
    @Test
    void captureHold_ValidRequest_ReturnsSuccess() {
        // Arrange
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .setDescription("Capture test")
                .setReleaseRemainder(true)
                .build();

        HoldResponseDto holdResponse = createTestHoldResponseDto(HoldResponse.StatusEnum.CAPTURED);
        when(holdService.captureHoldFunds(any(CaptureRequestDto.class))).thenReturn(holdResponse);

        // Act
        grpcHoldService.captureHold(request, responseObserver);

        // Assert
        ArgumentCaptor<CaptureRequestDto> requestCaptor = ArgumentCaptor.forClass(CaptureRequestDto.class);
        verify(holdService).captureHoldFunds(requestCaptor.capture());

        CaptureRequestDto capturedRequest = requestCaptor.getValue();
        assertEquals(TEST_HOLD_ID, capturedRequest.getHoldId());
        assertEquals("Capture test", capturedRequest.getDescription());
        assertTrue(capturedRequest.getReleaseRemainder());

        ArgumentCaptor<HoldResponseGRPC> responseCaptor = ArgumentCaptor.forClass(HoldResponseGRPC.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        HoldResponseGRPC response = responseCaptor.getValue();
        assertEquals(TEST_HOLD_ID, response.getHoldId());
        assertEquals(HoldResponseGRPC.HoldStatusGRPC.CAPTURED, response.getStatus());
    }

    @Test
    void captureHold_InvalidRequest_MissingHoldId_ReturnsError() {
        // Test with empty hold ID
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId("")
                .build();

        grpcHoldService.captureHold(request, responseObserver);
        verify(holdService, never()).captureHoldFunds(any());
        verifyErrorResponse(Code.INVALID_ARGUMENT, "Invalid request parameters");
        
        // Test with null hold ID
        request = CaptureHoldRequestGRPC.newBuilder().build();
                
        grpcHoldService.captureHold(request, responseObserver);
        verify(holdService, never()).captureHoldFunds(any());
        verifyErrorResponse(Code.INVALID_ARGUMENT, "Invalid request parameters");
    }

    @Test
    void captureHold_HoldNotFound_ReturnsError() {
        // Arrange
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId("NON_EXISTENT")
                .build();

        when(holdService.captureHoldFunds(any())).thenThrow(new HoldNotFoundException("Hold not found"));

        // Act
        grpcHoldService.captureHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.NOT_FOUND, "Hold not found");
    }

    @Test
    void captureHold_ExpiredHold_ReturnsError() {
        // Arrange
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .build();

        when(holdService.captureHoldFunds(any()))
            .thenThrow(new IllegalStateException("Hold has expired"));

        // Act
        grpcHoldService.captureHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.FAILED_PRECONDITION, "Hold has expired");
    }

    @Test
    void captureHold_InactiveHold_ReturnsError() {
        // Arrange
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .build();

        when(holdService.captureHoldFunds(any()))
            .thenThrow(new IllegalStateException("Hold is not active"));

        // Act
        grpcHoldService.captureHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.FAILED_PRECONDITION, "Hold is not in active state");
    }

    @Test
    void captureHold_ServiceError_ReturnsInternalError() {
        // Arrange
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .build();

        when(holdService.captureHoldFunds(any()))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        grpcHoldService.captureHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.INTERNAL, "Internal server error");
    }

    @Test
    void captureHold_AlreadyCaptured_ReturnsError() {
        // Arrange
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .build();

        when(holdService.captureHoldFunds(any()))
            .thenThrow(new IllegalStateException("Hold is not in active state"));

        // Act
        grpcHoldService.captureHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.FAILED_PRECONDITION, "Hold is not in active state");
    }

    @Test
    void releaseHold_InvalidRequest_MissingHoldId_ReturnsError() {
        // Arrange
        ReleaseHoldRequestGRPC request = ReleaseHoldRequestGRPC.newBuilder()
                .setHoldId("")
                .build();

        // Act
        grpcHoldService.releaseHold(request, responseObserver);

        // Assert
        verify(holdService, never()).releaseHold(any(), any());
        verifyErrorResponse(Code.INVALID_ARGUMENT, "Invalid request parameters");
    }

    @Test
    void releaseHold_HoldNotFound_ReturnsError() {
        // Arrange
        ReleaseHoldRequestGRPC request = ReleaseHoldRequestGRPC.newBuilder()
                .setHoldId("NON_EXISTENT")
                .build();

        when(holdService.releaseHold(eq("NON_EXISTENT"), any())).thenThrow(new HoldNotFoundException("Hold not found"));

        // Act
        grpcHoldService.releaseHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.NOT_FOUND, "Hold not found");
    }

    @Test
    void releaseHold_ServiceError_ReturnsInternalError() {
        // Arrange
        ReleaseHoldRequestGRPC request = ReleaseHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .build();

        when(holdService.releaseHold(eq(TEST_HOLD_ID), any())).thenThrow(new RuntimeException("Service error"));

        // Act
        grpcHoldService.releaseHold(request, responseObserver);

        // Assert
        verifyErrorResponse(Code.INTERNAL, "Internal server error");
    }


    private void assertResponseMatchesHoldResponse(HoldResponse expected, HoldResponseGRPC actual) {
        assertNotNull(actual);
        assertEquals(expected.getHoldId(), actual.getHoldId());
        assertEquals(expected.getUserCode(), actual.getUserCode());
        assertEquals(expected.getOriginalAmount(), actual.getOriginalAmount(), 0.001);
        assertEquals(expected.getRemainingAmount(), actual.getRemainingAmount(), 0.001);
        assertEquals(expected.getCapturedAmount(), actual.getCapturedAmount(), 0.001);
        assertEquals(expected.getStatus().name(), actual.getStatus().name());
        assertEquals(expected.getTransactionId(), actual.getTransactionId());
        assertEquals(expected.getDescription(), actual.getDescription());

        if (expected.getCreatedAt() != null) {
            assertEquals(expected.getCreatedAt().toEpochSecond(), actual.getCreatedAt().getSeconds());
            assertEquals(expected.getCreatedAt().getNano(), actual.getCreatedAt().getNanos());
        }
    }

    private void verifyErrorResponse(Status expectedStatus, String expectedMessage) {
        verifyErrorResponse(Code.forNumber(expectedStatus.getCode()), expectedMessage);
    }
}