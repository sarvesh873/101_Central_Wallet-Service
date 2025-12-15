//package com.central.wallet_service.grpc;
//
//import com.central.wallet_service.model.HoldStatus;
//import com.central.wallet_service.service.WalletService;
//import com.central.wallet_service.model.Wallet;
//import com.central.wallet_service.model.WalletUserSnapshot;
//import com.central.wallet_service.exception.WalletNotFoundException;
//import com.central.wallet_service.exception.InsufficientFundsException;
//import com.central.wallet_service.exception.DuplicateTransactionException;
//import com.central.wallet.*;
//import io.grpc.StatusRuntimeException;
//import io.grpc.stub.StreamObserver;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.openapitools.model.WalletResponse;
//import org.openapitools.model.WalletTransactionResponse;
//import java.time.OffsetDateTime;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class GrpcWalletServiceImplTest {
//
//    @Mock
//    private WalletService walletService;
//
//    @Mock
//    private StreamObserver<WalletResponseGRPC> walletResponseObserver;
//
//    @Mock
//    private StreamObserver<WalletTransactionResponseGRPC> transactionResponseObserver;
//
//    @InjectMocks
//    private GrpcWalletServiceImpl grpcWalletService;
//
//    private Wallet testWallet;
//    private WalletTransactionResponse testTransactionResponse;
//
//    @BeforeEach
//    void setUp() {
//        testWallet = new Wallet();
//        testWallet.setId(1L);
//        testWallet.setBalance(100.0);
//        testWallet.setAvailableBalance(80.0);
//        testWallet.setCurrency("USD");
//        testWallet.setWalletStatus(HoldStatus.ACTIVE);  // Initialize wallet status
//
//        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
//        userSnapshot.setUserCode("user123");
//        testWallet.setUserSnapshot(userSnapshot);
//
//        testTransactionResponse = new WalletTransactionResponse()
//                .walletId(1L)
//                .processedAmount(50.0)
//                .newBalance(150.0)
//                .newAvailableBalance(130.0)
//                .userCode("user123")
//                .status(WalletTransactionResponse.StatusEnum.COMPLETED);
//
//        // Reset mocks before each test
//        reset(walletService, walletResponseObserver, transactionResponseObserver);
//    }
//
//    @Test
//    void createWallet_ShouldCreateSuccessfully() {
//        // Arrange
//        WalletCreateRequestGRPC request = WalletCreateRequestGRPC.newBuilder()
//                .setUserCode("user123")
//                .setCurrency("USD")
//                .build();
//
//        org.openapitools.model.WalletResponse walletResponse = new org.openapitools.model.WalletResponse()
//                .walletId(1L)
//                .userCode("user123")
//                .balance(100.0)
//                .availableBalance(80.0)
//                .currency("USD")
//                .status("ACTIVE")
//                .username("testuser")
//                .email("test@example.com")
//                .phoneNumber("+1234567890");
//
//        when(walletService.createWallet(any())).thenReturn(walletResponse);
//
//        // Act
//        grpcWalletService.createWallet(request, walletResponseObserver);
//
//        // Assert
//        ArgumentCaptor<WalletResponseGRPC> responseCaptor = ArgumentCaptor.forClass(WalletResponseGRPC.class);
//        verify(walletResponseObserver).onNext(responseCaptor.capture());
//        verify(walletResponseObserver).onCompleted();
//
//        WalletResponseGRPC response = responseCaptor.getValue();
//        assertEquals("user123", response.getUserCode());
//        assertEquals("USD", response.getCurrency());
//        assertEquals(100.0, response.getBalance(), 0.001);
//        assertEquals(80.0, response.getAvailableBalance(), 0.001);
//        assertEquals("ACTIVE", response.getStatus());
//    }
//
//    @Test
//    void createWallet_WhenDuplicate_ShouldReturnError() {
//        // Arrange
//        WalletCreateRequestGRPC request = WalletCreateRequestGRPC.newBuilder()
//                .setUserCode("user123")
//                .setCurrency("USD")
//                .build();
//
//        when(walletService.createWallet(any()))
//                .thenThrow(new DuplicateTransactionException("Wallet exists"));
//
//        // Act
//        grpcWalletService.createWallet(request, walletResponseObserver);
//
//        // Assert
//        verify(walletResponseObserver, never()).onNext(any());
//        verify(walletResponseObserver, never()).onCompleted();
//        verify(walletResponseObserver).onError(any(StatusRuntimeException.class));
//    }
//
//
//    @Test
//    void getUserWallet_ValidUser_ReturnsWallet() {
//        // Arrange
//        GetWalletRequestGRPC request = GetWalletRequestGRPC.newBuilder()
//            .setUserCode("user123")
//            .build();
//
//        org.openapitools.model.WalletResponse walletResponse = new org.openapitools.model.WalletResponse()
//                .walletId(1L)
//                .userCode("user123")
//                .balance(100.0)
//                .availableBalance(80.0)
//                .currency("USD")
//                .status("ACTIVE")
//                .username("testuser")
//                .email("test@example.com")
//                .phoneNumber("+1234567890");
//
//        when(walletService.getWalletByUserCode("user123")).thenReturn(walletResponse);
//
//        // Act
//        grpcWalletService.getUserWallet(request, walletResponseObserver);
//
//        // Assert
//        ArgumentCaptor<WalletResponseGRPC> responseCaptor = ArgumentCaptor.forClass(WalletResponseGRPC.class);
//        verify(walletResponseObserver).onNext(responseCaptor.capture());
//        verify(walletResponseObserver).onCompleted();
//
//        WalletResponseGRPC response = responseCaptor.getValue();
//        assertNotNull(response);
//        assertEquals("user123", response.getUserCode());
//        assertEquals(100.0, response.getBalance(), 0.001);
//        assertEquals(80.0, response.getAvailableBalance(), 0.001);
//        assertEquals("USD", response.getCurrency());
//        assertEquals("ACTIVE", response.getStatus());
//    }
//
//    @Test
//    void getUserWallet_NonExistentUser_ThrowsException() {
//        // Arrange
//        GetWalletRequestGRPC request = GetWalletRequestGRPC.newBuilder()
//                .setUserCode("nonexistent")
//                .build();
//
//        when(walletService.getWalletByUserCode("nonexistent"))
//                .thenThrow(new WalletNotFoundException("Wallet not found"));
//
//        // Act
//        grpcWalletService.getUserWallet(request, walletResponseObserver);
//
//        // Assert
//        verify(walletResponseObserver, never()).onNext(any());
//        verify(walletResponseObserver, never()).onCompleted();
//        verify(walletResponseObserver).onError(any(StatusRuntimeException.class));
//    }
//
//    @Test
//    void depositFunds_ShouldProcessSuccessfully() {
//        // Arrange
//        WalletTransactionRequestGRPC request = WalletTransactionRequestGRPC.newBuilder()
//                .setUserCode("user123")
//                .setAmount(50.0)
//                .setCurrency("USD")
//                .build();
//
//        org.openapitools.model.WalletTransactionResponse depositResponse = new org.openapitools.model.WalletTransactionResponse()
//                .walletId(1L)
//                .transactionType(org.openapitools.model.WalletTransactionResponse.TransactionTypeEnum.DEPOSIT)
//                .processedAmount(50.0)
//                .newBalance(150.0)
//                .newAvailableBalance(130.0)
//                .userCode("user123")
//                .status(org.openapitools.model.WalletTransactionResponse.StatusEnum.COMPLETED)
//                .username("testuser")
//                .email("test@example.com")
//                .phoneNumber("+1234567890");
//
//        // Configure the mock before the test runs
//        when(walletService.depositFunds(eq("user123"), any())).thenReturn(depositResponse);
//
//        // Act
//        grpcWalletService.depositFunds(request, transactionResponseObserver);
//
//        // Assert
//        ArgumentCaptor<WalletTransactionResponseGRPC> captor = ArgumentCaptor.forClass(WalletTransactionResponseGRPC.class);
//        verify(transactionResponseObserver).onNext(captor.capture());
//        verify(transactionResponseObserver).onCompleted();
//
//        WalletTransactionResponseGRPC response = captor.getValue();
//        assertNotNull(response);
//        assertEquals(50.0, response.getProcessedAmount(), 0.001);
//        assertEquals(150.0, response.getNewBalance(), 0.001);
//        assertEquals(130.0, response.getNewAvailableBalance(), 0.001);
//        assertEquals("user123", response.getUserCode());
//        assertEquals(WalletTransactionResponseGRPC.TransactionTypeGRPC.DEPOSIT, response.getTransactionType());
//        assertEquals(WalletTransactionResponseGRPC.TransactionStatusGRPC.COMPLETED, response.getStatus());
//    }
//
//    @Test
//    void withdrawFunds_WhenInsufficientFunds_ShouldReturnError() {
//        // Arrange
//        WalletTransactionRequestGRPC request = WalletTransactionRequestGRPC.newBuilder()
//                .setUserCode("user123")
//                .setAmount(200.0)
//                .build();
//
//        when(walletService.withdrawFunds(anyString(), any()))
//                .thenThrow(new InsufficientFundsException("Insufficient funds"));
//
//        // Act
//        grpcWalletService.withdrawFunds(request, transactionResponseObserver);
//
//        // Assert
//        verify(transactionResponseObserver, never()).onNext(any());
//        verify(transactionResponseObserver, never()).onCompleted();
//        verify(transactionResponseObserver).onError(any(StatusRuntimeException.class));
//    }
//
//
//    // Removed createWalletResponse method as we're now creating response objects directly in the tests
//}
