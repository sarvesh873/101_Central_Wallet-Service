package com.central.wallet_service.grpc;

import com.central.wallet.*;
import com.central.wallet_service.exception.HoldNotFoundException;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.service.HoldService;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrpcHoldServiceImplTest {

    @Mock
    private HoldService holdService;

    @Mock
    private StreamObserver<HoldResponseGRPC> responseObserver;

    @InjectMocks
    private GrpcHoldServiceImpl grpcHoldService;

    private static final String TEST_HOLD_ID = "HOLD123";
    private static final String TEST_USER_CODE = "USER123";
    private static final String TEST_TXN_ID = "TXN123";
    private static final double TEST_AMOUNT = 100.0;
    
    private HoldResponse createTestHoldResponse(HoldResponse.StatusEnum status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new HoldResponse()
            .holdId(TEST_HOLD_ID)
            .userCode(TEST_USER_CODE)
            .originalAmount(TEST_AMOUNT)
            .remainingAmount(TEST_AMOUNT)
            .capturedAmount(0.0)
            .status(status)
            .transactionId(TEST_TXN_ID)
            .createdAt(now)
            .updatedAt(now)
            .expiresAt(now.plusDays(1))
            .description("Test hold");
    }
    
    private Timestamp toTimestamp(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        Instant instant = dateTime.toInstant();
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    @Test
    void placeHold_ValidRequest_ReturnsSuccess() {
        // Arrange
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(TEST_AMOUNT)
                .setTransactionId(TEST_TXN_ID)
                .build();

        HoldResponse holdResponse = createTestHoldResponse(HoldResponse.StatusEnum.ACTIVE);
        when(holdService.placeHold(any(HoldRequest.class))).thenReturn(holdResponse);

        // Act
        grpcHoldService.placeHold(request, responseObserver);

        // Assert
        ArgumentCaptor<HoldRequest> requestCaptor = ArgumentCaptor.forClass(HoldRequest.class);
        verify(holdService).placeHold(requestCaptor.capture());
        
        HoldRequest capturedRequest = requestCaptor.getValue();
        assertEquals(TEST_USER_CODE, capturedRequest.getUserCode());
        assertEquals(TEST_AMOUNT, capturedRequest.getAmount());
        assertEquals(TEST_TXN_ID, capturedRequest.getTransactionId());
        
        ArgumentCaptor<HoldResponseGRPC> responseCaptor = ArgumentCaptor.forClass(HoldResponseGRPC.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        
        HoldResponseGRPC response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(TEST_HOLD_ID, response.getHoldId());
        assertEquals(TEST_USER_CODE, response.getUserCode());
        assertEquals(TEST_AMOUNT, response.getOriginalAmount());
        assertEquals(HoldResponseGRPC.HoldStatusGRPC.ACTIVE, response.getStatus());
    }

    @Test
    void placeHold_InvalidRequest_ReturnsError() {
        // Arrange
        PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                .setUserCode("")  // Invalid user code
                .build();

        // Act
        grpcHoldService.placeHold(request, responseObserver);

        // Assert
        verify(holdService, never()).placeHold(any());
        
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();

        StatusRuntimeException exception = (StatusRuntimeException) errorCaptor.getValue();
        assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        // Check for either of the possible error messages
        String errorDescription = exception.getStatus().getDescription();
        assertTrue(errorDescription.contains("Invalid request parameters") || 
                  errorDescription.contains("Amount must be greater than zero"), 
                  "Unexpected error message: " + errorDescription);
    }

    @Test
    void captureHold_ValidRequest_ReturnsSuccess() {
        // Arrange
        String description = "Test capture";
        boolean releaseRemainder = true;
        
        CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .setDescription(description)
                .setReleaseRemainder(releaseRemainder)
                .build();

        HoldResponse holdResponse = createTestHoldResponse(HoldResponse.StatusEnum.CAPTURED);
        when(holdService.captureHoldFunds(any(CaptureRequest.class))).thenReturn(holdResponse);

        // Act
        grpcHoldService.captureHold(request, responseObserver);

        // Assert
        ArgumentCaptor<CaptureRequest> requestCaptor = ArgumentCaptor.forClass(CaptureRequest.class);
        verify(holdService).captureHoldFunds(requestCaptor.capture());
        
        CaptureRequest capturedRequest = requestCaptor.getValue();
        assertEquals(TEST_HOLD_ID, capturedRequest.getHoldId());
        assertEquals(description, capturedRequest.getDescription());
        assertEquals(releaseRemainder, capturedRequest.getReleaseRemainder());
        
        ArgumentCaptor<HoldResponseGRPC> responseCaptor = ArgumentCaptor.forClass(HoldResponseGRPC.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        
        HoldResponseGRPC response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(TEST_HOLD_ID, response.getHoldId());
        assertEquals(HoldResponseGRPC.HoldStatusGRPC.CAPTURED, response.getStatus());
    }

    @Test
    void releaseHold_ValidRequest_ReturnsSuccess() {
        // Arrange
        String reason = "Test release";
        ReleaseHoldRequestGRPC request = ReleaseHoldRequestGRPC.newBuilder()
                .setHoldId(TEST_HOLD_ID)
                .setReason(reason)
                .putMetadata("key1", "value1")
                .putMetadata("key2", "value2")
                .build();

        HoldResponse holdResponse = createTestHoldResponse(HoldResponse.StatusEnum.RELEASED);
        when(holdService.releaseHold(eq(TEST_HOLD_ID), any(ReleaseHoldRequest.class)))
            .thenReturn(holdResponse);

        // Act
        grpcHoldService.releaseHold(request, responseObserver);

        // Assert
        ArgumentCaptor<ReleaseHoldRequest> requestCaptor = ArgumentCaptor.forClass(ReleaseHoldRequest.class);
        verify(holdService).releaseHold(eq(TEST_HOLD_ID), requestCaptor.capture());
        
        ReleaseHoldRequest capturedRequest = requestCaptor.getValue();
        assertEquals(reason, capturedRequest.getReason());
        assertNotNull(capturedRequest.getMetadata());
        assertEquals(2, capturedRequest.getMetadata().size());
        assertEquals("value1", capturedRequest.getMetadata().get("key1"));
        assertEquals("value2", capturedRequest.getMetadata().get("key2"));
        
        ArgumentCaptor<HoldResponseGRPC> responseCaptor = ArgumentCaptor.forClass(HoldResponseGRPC.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        
        HoldResponseGRPC response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(TEST_HOLD_ID, response.getHoldId());
        assertEquals(HoldResponseGRPC.HoldStatusGRPC.RELEASED, response.getStatus());
    }
}
