package com.central.wallet_service.grpc;

import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.service.WalletService;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletUserSnapshot;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.exception.InsufficientFundsException;
import com.central.wallet_service.exception.DuplicateTransactionException;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto.TransactionType;
import com.central.wallet_service.dto.WalletTransactionResponseDto.TransactionStatus;
import com.central.wallet_service.dto.adapter.response.GrpcWalletResponseAdapter;
import com.central.wallet_service.dto.adapter.response.GrpcTransactionResponseAdapter;
import com.central.wallet.*;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrpcWalletServiceImplTest {

    @Mock
    private WalletService walletService;

    @Mock
    private StreamObserver<WalletResponseGRPC> walletResponseObserver;

    @Mock
    private StreamObserver<WalletTransactionResponseGRPC> transactionResponseObserver;

    @InjectMocks
    private GrpcWalletServiceImpl grpcWalletService;

    private static final String TEST_USER_CODE = "user123";
    private static final String TEST_CURRENCY = "USD";
    private static final double TEST_AMOUNT = 50.0;

    @BeforeEach
    void setUp() {
        reset(walletService, walletResponseObserver, transactionResponseObserver);
    }

    // Helper method to create a test wallet DTO
    private WalletResponseDto createTestWalletResponse() {
        return new WalletResponseDto() {
            @Override public Long getWalletId() { return 1L; }
            @Override public String getUserCode() { return TEST_USER_CODE; }
            @Override public Double getBalance() { return 100.0; }
            @Override public String getStatus() { return "ACTIVE"; }
            @Override public String getCurrency() { return TEST_CURRENCY; }
            @Override public Double getAvailableBalance() { return 80.0; }
            @Override public OffsetDateTime getCreatedAt() { return OffsetDateTime.now(); }
            @Override public String getUsername() { return "testuser"; }
            @Override public String getEmail() { return "test@example.com"; }
            @Override public String getPhoneNumber() { return "+1234567890"; }
        };
    }

    // Helper method to create a test transaction response DTO
    private WalletTransactionResponseDto createTestTransactionResponse(TransactionType type) {
        return new WalletTransactionResponseDto() {
            @Override public Long getWalletId() { return 1L; }
            @Override public TransactionType getTransactionType() { return type; }
            @Override public Double getProcessedAmount() { return TEST_AMOUNT; }
            @Override public Double getNewBalance() {
                return type == TransactionType.DEPOSIT ? 150.0 : 50.0;
            }
            @Override public Double getNewAvailableBalance() {
                return type == TransactionType.DEPOSIT ? 130.0 : 30.0;
            }
            @Override public String getUserCode() { return TEST_USER_CODE; }
            @Override public TransactionStatus getStatus() { return TransactionStatus.COMPLETED; }
            @Override public String getUsername() { return "testuser"; }
            @Override public String getEmail() { return "test@example.com"; }
            @Override public String getPhoneNumber() { return "+1234567890"; }
        };
    }

    @Test
    void createWallet_ShouldCreateSuccessfully() {
        // Arrange
        WalletCreateRequestGRPC request = WalletCreateRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setCurrency(TEST_CURRENCY)
                .build();

        WalletResponseDto walletResponse = createTestWalletResponse();
        when(walletService.createWallet(any())).thenReturn(walletResponse);

        // Act
        grpcWalletService.createWallet(request, walletResponseObserver);

        // Assert
        ArgumentCaptor<WalletResponseGRPC> responseCaptor = ArgumentCaptor.forClass(WalletResponseGRPC.class);
        verify(walletResponseObserver).onNext(responseCaptor.capture());
        verify(walletResponseObserver).onCompleted();

        WalletResponseGRPC response = responseCaptor.getValue();
        assertEquals(TEST_USER_CODE, response.getUserCode());
        assertEquals(TEST_CURRENCY, response.getCurrency());
        assertEquals(100.0, response.getBalance(), 0.001);
        assertEquals(80.0, response.getAvailableBalance(), 0.001);
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void getUserWallet_ValidUser_ReturnsWallet() {
        // Arrange
        GetWalletRequestGRPC request = GetWalletRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .build();

        WalletResponseDto walletResponse = createTestWalletResponse();
        when(walletService.getWalletByUserCode(TEST_USER_CODE)).thenReturn(walletResponse);

        // Act
        grpcWalletService.getUserWallet(request, walletResponseObserver);

        // Assert
        ArgumentCaptor<WalletResponseGRPC> responseCaptor = ArgumentCaptor.forClass(WalletResponseGRPC.class);
        verify(walletResponseObserver).onNext(responseCaptor.capture());
        verify(walletResponseObserver).onCompleted();

        WalletResponseGRPC response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(TEST_USER_CODE, response.getUserCode());
        assertEquals(100.0, response.getBalance(), 0.001);
        assertEquals(80.0, response.getAvailableBalance(), 0.001);
        assertEquals(TEST_CURRENCY, response.getCurrency());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void depositFunds_ShouldProcessSuccessfully() {
        // Arrange
        WalletTransactionRequestGRPC request = WalletTransactionRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(TEST_AMOUNT)
                .setCurrency(TEST_CURRENCY)
                .build();

        WalletTransactionResponseDto depositResponse = createTestTransactionResponse(TransactionType.DEPOSIT);
        when(walletService.depositFunds(eq(TEST_USER_CODE), any())).thenReturn(depositResponse);

        // Act
        grpcWalletService.depositFunds(request, transactionResponseObserver);

        // Assert
        ArgumentCaptor<WalletTransactionResponseGRPC> captor =
                ArgumentCaptor.forClass(WalletTransactionResponseGRPC.class);
        verify(transactionResponseObserver).onNext(captor.capture());
        verify(transactionResponseObserver).onCompleted();

        WalletTransactionResponseGRPC response = captor.getValue();
        assertNotNull(response);
        assertEquals(TEST_AMOUNT, response.getProcessedAmount(), 0.001);
        assertEquals(150.0, response.getNewBalance(), 0.001);
        assertEquals(130.0, response.getNewAvailableBalance(), 0.001);
        assertEquals(TEST_USER_CODE, response.getUserCode());
        assertEquals(WalletTransactionResponseGRPC.TransactionTypeGRPC.DEPOSIT, response.getTransactionType());
        assertEquals(WalletTransactionResponseGRPC.TransactionStatusGRPC.COMPLETED, response.getStatus());
    }

    @Test
    void withdrawFunds_ShouldProcessSuccessfully() {
        // Arrange
        WalletTransactionRequestGRPC request = WalletTransactionRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(TEST_AMOUNT)
                .setCurrency(TEST_CURRENCY)
                .build();

        WalletTransactionResponseDto withdrawResponse = createTestTransactionResponse(TransactionType.WITHDRAWAL);
        when(walletService.withdrawFunds(eq(TEST_USER_CODE), any())).thenReturn(withdrawResponse);

        // Act
        grpcWalletService.withdrawFunds(request, transactionResponseObserver);

        // Assert
        ArgumentCaptor<WalletTransactionResponseGRPC> captor =
                ArgumentCaptor.forClass(WalletTransactionResponseGRPC.class);
        verify(transactionResponseObserver).onNext(captor.capture());
        verify(transactionResponseObserver).onCompleted();

        WalletTransactionResponseGRPC response = captor.getValue();
        assertNotNull(response);
        assertEquals(TEST_AMOUNT, response.getProcessedAmount(), 0.001);
        assertEquals(50.0, response.getNewBalance(), 0.001);
        assertEquals(30.0, response.getNewAvailableBalance(), 0.001);
        assertEquals(TEST_USER_CODE, response.getUserCode());
        assertEquals(WalletTransactionResponseGRPC.TransactionTypeGRPC.WITHDRAWAL, response.getTransactionType());
        assertEquals(WalletTransactionResponseGRPC.TransactionStatusGRPC.COMPLETED, response.getStatus());
    }

    @Test
    void withdrawFunds_WhenInsufficientFunds_ShouldReturnError() {
        // Arrange
        WalletTransactionRequestGRPC request = WalletTransactionRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setAmount(200.0)
                .setCurrency(TEST_CURRENCY)
                .build();

        when(walletService.withdrawFunds(eq(TEST_USER_CODE), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        // Act
        grpcWalletService.withdrawFunds(request, transactionResponseObserver);

        // Assert
        verify(transactionResponseObserver, never()).onNext(any());
        verify(transactionResponseObserver, never()).onCompleted();
        verify(transactionResponseObserver).onError(any(StatusRuntimeException.class));
    }

    @Test
    void createWallet_WhenDuplicate_ShouldReturnError() {
        // Arrange
        WalletCreateRequestGRPC request = WalletCreateRequestGRPC.newBuilder()
                .setUserCode(TEST_USER_CODE)
                .setCurrency(TEST_CURRENCY)
                .build();

        when(walletService.createWallet(any()))
                .thenThrow(new DuplicateTransactionException("Wallet exists"));

        // Act
        grpcWalletService.createWallet(request, walletResponseObserver);

        // Assert
        verify(walletResponseObserver, never()).onNext(any());
        verify(walletResponseObserver, never()).onCompleted();
        verify(walletResponseObserver).onError(any(StatusRuntimeException.class));
    }
}