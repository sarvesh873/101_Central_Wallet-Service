//package com.central.wallet_service.service;
//
//import com.central.wallet_service.constants.WalletConstants;
//import com.central.wallet_service.dto.*;
//import com.central.wallet_service.exception.*;
//import com.central.wallet_service.model.*;
//import com.central.wallet_service.repository.WalletHoldRepository;
//import com.central.wallet_service.repository.WalletRepository;
//import com.central.wallet_service.specifications.WalletHoldSpecifications;
//import com.central.wallet_service.utils.ServiceUtils;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.openapitools.model.*;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.OffsetDateTime;
//import java.util.Collections;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class HoldServiceImplTest {
//
//    @Mock
//    private WalletHoldRepository holdRepository;
//
//    @Mock
//    private WalletRepository walletRepository;
//
//    @InjectMocks
//    private HoldServiceImpl holdService;
//
//    private Wallet testWallet;
//    private WalletHold testHold;
//    private PlaceHoldRequestDto holdRequest;
//    private CaptureRequestDto captureRequest;
//    private ReleaseHoldRequestDto releaseRequest;
//    private ExtendHoldRequestDto extendRequest;
//    private AdjustHoldRequestDto adjustRequest;
//
//    @BeforeEach
//    void setUp() {
//        // Setup test wallet
//        testWallet = new Wallet();
//        testWallet.setId(1L);
//        testWallet.setBalance(1000.0);
//        testWallet.setAvailableBalance(1000.0);
//        testWallet.setCurrency("USD");
//        testWallet.setWalletStatus(HoldStatus.ACTIVE);
//        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
//        userSnapshot.setUserCode("USER123");
//        testWallet.setUserSnapshot(userSnapshot);
//
//        // Setup test hold
//        testHold = WalletHold.builder()
//                .id(1L)
//                .holdId("HOLD123")
//                .wallet(testWallet)
//                .transaction_id("TXN123")
//                .originalAmount(100.0)
//                .remainingAmount(50.0)
//                .capturedAmount(50.0)
//                .status(HoldStatus.ACTIVE)
//                .description("Test hold")
//                .expiresAt(LocalDateTime.now().plusDays(1))
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//        holdRequest = PlaceHoldRequestDto.builder()
//                .userCode("USER123")
//                .amount(100.0)
//                .transactionId("TXN123")
//                .description("Test hold")
//                .build();
//
//        captureRequest = CaptureRequestDto.builder()
//                .holdId("HOLD123")
//                .build();
//
//        releaseRequest = ReleaseHoldRequestDto.builder()
//                .reason("Test release")
//                .build();
//
//        extendRequest = ExtendHoldRequestDto.builder()
//                .newExpiryTime(OffsetDateTime.now().plusDays(2))
//                .build();
//
//        adjustRequest = AdjustHoldRequestDto.builder()
//                .newAmount(150.0)
//                .reason("Adjustment needed")
//                .build();
//    }
//
//    @Test
//    void placeHold_ValidRequest_ReturnsHoldResponse() {
//        // Arrange
//        when(walletRepository.findByUserCode(anyString())).thenReturn(Optional.of(testWallet));
//        when(holdRepository.save(any(WalletHold.class))).thenReturn(testHold);
//        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
//
//        // Act
//        HoldResponse response = holdService.placeHold(holdRequest);
//
//        // Assert
//        assertNotNull(response);
//        assertEquals("HOLD123", response.getHoldId());
//        assertEquals(100.0, response.getOriginalAmount());
//        assertEquals(50.0, response.getCapturedAmount());
//        assertEquals(HoldResponse.StatusEnum.ACTIVE, response.getStatus());
//
//        verify(holdRepository, times(1)).save(any(WalletHold.class));
//        verify(walletRepository, times(1)).save(any(Wallet.class));
//    }
//
//    @Test
//    void placeHold_WalletNotFound_ThrowsException() {
//        // Arrange
//        when(walletRepository.findByUserCode(anyString())).thenReturn(Optional.empty());
//
//        // Act & Assert
//        WalletNotFoundException exception = assertThrows(WalletNotFoundException.class,
//                () -> holdService.placeHold(holdRequest));
//
//        assertTrue(exception.getMessage().contains("not found"));
//        verify(holdRepository, never()).save(any(WalletHold.class));
//    }
//
//    @Test
//    void placeHold_InsufficientFunds_ThrowsException() {
//        // Arrange
//        testWallet.setAvailableBalance(50.0);
//        when(walletRepository.findByUserCode(anyString())).thenReturn(Optional.of(testWallet));
//
//        // Act & Assert
//        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
//                () -> holdService.placeHold(holdRequest));
//
//        assertTrue(exception.getMessage().toLowerCase().contains("insufficient") ||
//                  exception.getMessage().contains("Insufficient"));
//        verify(holdRepository, never()).save(any(WalletHold.class));
//    }
//
//    @Test
//    void placeHold_DuplicateTransaction_ThrowsException() {
//        // Arrange
//        when(walletRepository.findByUserCode(anyString())).thenReturn(Optional.of(testWallet));
//        when(holdRepository.save(any(WalletHold.class)))
//                .thenThrow(new DataIntegrityViolationException("duplicate key"));
//
//        // Act & Assert
//        Exception exception = assertThrows(Exception.class,
//                () -> holdService.placeHold(holdRequest));
//
//        // Check if the exception or its cause is DuplicateTransactionException
//        assertTrue(exception instanceof DuplicateTransactionException ||
//                  (exception.getCause() != null && exception.getCause() instanceof DuplicateTransactionException));
//
//        verify(holdRepository, times(1)).save(any(WalletHold.class));
//    }
//
//    @Test
//    void captureHoldFunds_ValidRequest_ReturnsUpdatedHold() {
//        // Arrange
//        when(holdRepository.findByHoldId(anyString())).thenReturn(Optional.of(testHold));
//        when(holdRepository.save(any(WalletHold.class))).thenReturn(testHold);
//        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
//
//        // Act
//        HoldResponse response = holdService.captureHoldFunds(captureRequest);
//
//        // Assert
//        assertNotNull(response);
//        assertEquals(HoldResponse.StatusEnum.CAPTURED, response.getStatus());
//        verify(holdRepository, times(1)).save(any(WalletHold.class));
//        verify(walletRepository, times(1)).save(any(Wallet.class));
//    }
//
//    @Test
//    void captureHoldFunds_HoldNotFound_ThrowsException() {
//        // Arrange
//        when(holdRepository.findByHoldId(anyString())).thenReturn(Optional.empty());
//
//        // Act & Assert
//        HoldNotFoundException exception = assertThrows(HoldNotFoundException.class,
//                () -> holdService.captureHoldFunds(captureRequest));
//
//        assertTrue(exception.getMessage().contains("not found"));
//        verify(holdRepository, never()).save(any(WalletHold.class));
//    }
//
//    @Test
//    void captureHoldFunds_AlreadyProcessed_ThrowsException() {
//        // Arrange
//        testHold.setStatus(HoldStatus.CAPTURED);
//        when(holdRepository.findByHoldId(anyString())).thenReturn(Optional.of(testHold));
//
//        // Act
//        Exception exception = assertThrows(Exception.class,
//                () -> holdService.captureHoldFunds(captureRequest));
//
//        // Assert
//        String errorMessage = exception.getMessage() != null ? exception.getMessage() :
//                            (exception.getCause() != null ? exception.getCause().getMessage() : "");
//        assertTrue(errorMessage.toLowerCase().contains("already") ||
//                  errorMessage.toLowerCase().contains("processed") ||
//                  errorMessage.toLowerCase().contains("captured") ||
//                  errorMessage.toLowerCase().contains("completed"));
//
//        verify(holdRepository, never()).save(any(WalletHold.class));
//    }
//
//    @Test
//    void releaseHold_ValidRequest_ReturnsUpdatedHold() {
//        // Arrange
//        when(holdRepository.findByHoldId(anyString())).thenReturn(Optional.of(testHold));
//        when(holdRepository.save(any(WalletHold.class))).thenReturn(testHold);
//        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
//
//        // Act
//        HoldResponse response = holdService.releaseHold("HOLD123", releaseRequest);
//
//        // Assert
//        assertNotNull(response);
//        assertEquals(HoldResponse.StatusEnum.RELEASED, response.getStatus());
//        verify(holdRepository, times(1)).save(any(WalletHold.class));
//        verify(walletRepository, times(1)).save(any(Wallet.class));
//    }
//
//    @Test
//    void extendHold_ValidRequest_ReturnsUpdatedHold() {
//        // Arrange
//        when(holdRepository.findByHoldId(anyString())).thenReturn(Optional.of(testHold));
//        when(holdRepository.save(any(WalletHold.class))).thenReturn(testHold);
//        OffsetDateTime newExpiry = OffsetDateTime.now().plusDays(2);
//        extendRequest.setNewExpiresAt(newExpiry);
//
//        // Act
//        HoldResponse response = holdService.extendHold("HOLD123", extendRequest);
//
//        // Assert
//        assertNotNull(response);
//        verify(holdRepository, times(1)).save(any(WalletHold.class));
//    }
//
//    @Test
//    void adjustHold_ValidRequest_ReturnsUpdatedHold() {
//        // Arrange
//        // Set up test data with sufficient balance
//        testWallet.setBalance(1000.0); // Set total balance
//        testWallet.setAvailableBalance(500.0); // Set available balance
//        testHold.setRemainingAmount(400.0); // Set remaining amount in the hold
//        testHold.setCapturedAmount(100.0); // Current hold amount
//        testHold.setStatus(HoldStatus.ACTIVE);
//
//        // Mock repository responses
//        when(holdRepository.findByHoldId(anyString())).thenReturn(Optional.of(testHold));
//        when(holdRepository.save(any(WalletHold.class))).thenAnswer(invocation -> {
//            WalletHold savedHold = invocation.getArgument(0);
//            return savedHold; // Return the saved hold as is
//        });
//        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
//            Wallet savedWallet = invocation.getArgument(0);
//            return savedWallet; // Return the saved wallet as is
//        });
//
//        // Create an adjustment request to decrease the hold amount
//        AdjustHoldRequest adjustRequest = new AdjustHoldRequest()
//            .newAmount(80.0)  // Decrease hold amount from 100 to 80
//            .reason("Test adjustment");
//
//        // Act
//        HoldResponse response = holdService.adjustHold("HOLD123", adjustRequest);
//
//        // Assert
//        assertNotNull(response);
//        assertEquals(HoldResponse.StatusEnum.ACTIVE, response.getStatus());
//        verify(holdRepository, times(1)).save(any(WalletHold.class));
//        verify(walletRepository, times(1)).save(any(Wallet.class));
//    }
//
//    @Test
//    void getHold_ValidId_ReturnsHold() {
//        // Arrange
//        when(holdRepository.findByHoldId(anyString())).thenReturn(Optional.of(testHold));
//
//        // Act
//        HoldResponse response = holdService.getHold("HOLD123");
//
//        // Assert
//        assertNotNull(response);
//        assertEquals("HOLD123", response.getHoldId());
//    }
//
//    @Test
//    void listHolds_WithFilters_ReturnsFilteredHolds() {
//        // Arrange
//        Page<WalletHold> page = new PageImpl<>(Collections.singletonList(testHold));
//        when(holdRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
//
//        // Act
//        Page<HoldResponse> result = holdService.listHolds(
//                "USER123",
//                "ACTIVE",
//                "USD",
//                OffsetDateTime.now().minusDays(1),
//                OffsetDateTime.now().plusDays(1),
//                PageRequest.of(0, 10)
//        );
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getTotalElements());
//        verify(holdRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
//    }
//
//    @Test
//    void listHolds_NoFilters_ReturnsAllHolds() {
//        // Arrange
//        Page<WalletHold> page = new PageImpl<>(Collections.singletonList(testHold));
//        when(holdRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
//
//        // Act
//        Page<HoldResponse> result = holdService.listHolds(
//            null, null, null, null, null, PageRequest.of(0, 10));
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getTotalElements());
//        verify(holdRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
//    }
//}
