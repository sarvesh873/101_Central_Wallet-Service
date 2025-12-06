package com.central.wallet_service.service;

import com.central.wallet_service.constants.WalletConstants;
import com.central.wallet_service.exception.DuplicateTransactionException;
import com.central.wallet_service.exception.InsufficientFundsException;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletUserSnapshot;
import com.central.wallet_service.repository.WalletRepository;
import com.central.wallet_service.repository.WalletUserSnapshotRepository;
import com.central.wallet_service.utils.ServiceUtils; // Assuming this is needed for toOffsetDateTime
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.WalletCreateRequest;
import org.openapitools.model.WalletTransactionRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    private static final String TEST_USER_CODE = "TEST_USER_123";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final double DEFAULT_INITIAL_BALANCE = 100.0;
    private static final double TRANSACTION_AMOUNT = 50.0;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletUserSnapshotRepository userSnapshotRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private WalletUserSnapshot userSnapshot;
    private Wallet wallet;
    private WalletCreateRequest createRequest;
    private WalletTransactionRequest transactionRequest;

    @BeforeEach
    void setUp() {
        // Mock the static utility method for time conversion for response mapping
        try (MockedStatic<ServiceUtils> mockedServiceUtils = Mockito.mockStatic(ServiceUtils.class)) {
            mockedServiceUtils.when(() -> ServiceUtils.toOffsetDateTime(any(LocalDateTime.class)))
                    .thenReturn(OffsetDateTime.now());
        }

        userSnapshot = WalletUserSnapshot.builder()
                .userCode(TEST_USER_CODE)
                .username(TEST_USER_CODE)
                .email(TEST_USER_CODE + "@example.com")
                .phoneNumber("+1234567890")
                .build();

        wallet = Wallet.builder()
                .id(100L)
                .userSnapshot(userSnapshot)
                .balance(DEFAULT_INITIAL_BALANCE)
                .availableBalance(DEFAULT_INITIAL_BALANCE)
                .currency(DEFAULT_CURRENCY)
                .walletStatus(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastModifiedBy(TEST_USER_CODE)
                .build();

        createRequest = new WalletCreateRequest()
                .userCode(TEST_USER_CODE)
                .currency(DEFAULT_CURRENCY);

        transactionRequest = new WalletTransactionRequest()
                .amount(TRANSACTION_AMOUNT);
    }

    // --- createWallet Tests ---

    @Test
    void createWallet_Success() {
        // Arrange
        when(walletRepository.existsByUserCode(TEST_USER_CODE)).thenReturn(false);
        when(userSnapshotRepository.save(any(WalletUserSnapshot.class))).thenReturn(userSnapshot);
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        // Act
        var response = walletService.createWallet(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_USER_CODE, response.getUserCode());
        assertEquals(DEFAULT_INITIAL_BALANCE, response.getBalance());
        verify(walletRepository).existsByUserCode(TEST_USER_CODE);
        verify(userSnapshotRepository).save(any(WalletUserSnapshot.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_ThrowsIllegalStateException_IfWalletExists() {
        // Arrange
        when(walletRepository.existsByUserCode(TEST_USER_CODE)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> walletService.createWallet(createRequest));
        verify(walletRepository).existsByUserCode(TEST_USER_CODE);
        verify(userSnapshotRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    // Validation Failures for createWallet
    @Test
    void createWallet_ThrowsIllegalArgumentException_IfRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(null));
    }

    @Test
    void createWallet_ThrowsIllegalArgumentException_IfUserCodeIsBlank() {
        createRequest.userCode("");
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(createRequest));
    }

    @Test
    void createWallet_ThrowsIllegalArgumentException_IfCurrencyIsNull() {
        createRequest.currency(null); // Already tested for default, this tests the explicit check for null currency in validateWalletRequest
        // The service logic defaults null currency, so let's adjust the mock to ensure the validation path is covered if it wasn't for defaulting.
        // Re-reading service: validateWalletRequest throws if currency == null. The check is: if (request.getCurrency() == null) throw...
        // The save path defaults it. Let's make sure the validator is hit first.
        WalletCreateRequest requestInvalidCurrency = new WalletCreateRequest()
                .userCode(TEST_USER_CODE)
                .currency(null);

        // This test hits the validation block at the start, which is a bit of a contradiction with the later default logic.
        // Based on the provided code, if currency is null, it throws, but if it passes validation, it defaults. Let's cover the explicit validation path.
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(requestInvalidCurrency));
    }

    // Exception Handling for createWallet
    @Test
    void createWallet_ThrowsRuntimeException_OnOtherDataIntegrityViolation() {
        // Arrange
        when(walletRepository.existsByUserCode(TEST_USER_CODE)).thenReturn(false);
        when(userSnapshotRepository.save(any(WalletUserSnapshot.class))).thenReturn(userSnapshot);
        // Mock a DataIntegrityViolationException, but without the 'duplicate' message
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Some other DB error", new Throwable("Transaction processing failed"));
        when(walletRepository.save(any(Wallet.class))).thenThrow(ex);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> walletService.createWallet(createRequest));
        // Note: The service catches generic Exception and rewraps as RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR, ex)
        // for createWallet, but for deposit/withdraw it has specific handling.
        // Let's verify based on implementation.
        // In createWallet: catch (DataIntegrityViolationException ex) { ... if (!duplicate) throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR, ex); }
        assertEquals(WalletConstants.INTERNAL_SERVER_ERROR, thrown.getMessage());
    }



    // --- getWalletByUserCode Tests ---

    @Test
    void getWalletByUserCode_Success() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));

        // Act
        var response = walletService.getWalletByUserCode(TEST_USER_CODE);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_USER_CODE, response.getUserCode());
        verify(walletRepository).findByUserCode(TEST_USER_CODE);
    }

    @Test
    void getWalletByUserCode_ThrowsIllegalArgumentException_IfUserCodeIsBlank() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> walletService.getWalletByUserCode(""));
    }

    @Test
    void getWalletByUserCode_ThrowsWalletNotFoundException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletByUserCode(TEST_USER_CODE));
    }

    @Test
    void getWalletByUserCode_ThrowsIllegalStateException_IfWalletInactive() {
        // Arrange
        wallet.setWalletStatus(HoldStatus.FROZEN);
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> walletService.getWalletByUserCode(TEST_USER_CODE));
    }

    @Test
    void getWalletByUserCode_ThrowsRuntimeException_OnJpaSystemException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenThrow(new JpaSystemException(new RuntimeException("DB Connection Lost")));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> walletService.getWalletByUserCode(TEST_USER_CODE));
    }

    @Test
    void getWalletByUserCode_ThrowsRuntimeException_OnUnexpectedException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenThrow(new NullPointerException("NPE"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.getWalletByUserCode(TEST_USER_CODE));
        assertEquals(WalletConstants.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // --- depositFunds Tests ---

    @Test
    void depositFunds_Success() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        // Act
        var response = walletService.depositFunds(TEST_USER_CODE, transactionRequest);

        // Assert
        assertNotNull(response);
        assertEquals(DEFAULT_INITIAL_BALANCE + TRANSACTION_AMOUNT, wallet.getBalance());
        assertEquals(DEFAULT_INITIAL_BALANCE + TRANSACTION_AMOUNT, wallet.getAvailableBalance());
        assertEquals(TRANSACTION_AMOUNT, response.getProcessedAmount());
        verify(walletRepository).save(any(Wallet.class));
    }

    // Deposit Validation Failures
    @Test
    void depositFunds_ThrowsIllegalArgumentException_IfUserCodeIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> walletService.depositFunds("", transactionRequest));
    }

    @Test
    void depositFunds_ThrowsIllegalArgumentException_IfRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> walletService.depositFunds(TEST_USER_CODE, null));
    }

    @Test
    void depositFunds_ThrowsIllegalArgumentException_IfAmountIsZero() {
        transactionRequest.amount(0.0);
        assertThrows(IllegalArgumentException.class, () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));
    }

    @Test
    void depositFunds_ThrowsIllegalArgumentException_IfAmountIsNegative() {
        transactionRequest.amount(-10.0);
        assertThrows(IllegalArgumentException.class, () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));
    }

    @Test
    void depositFunds_ThrowsWalletNotFoundException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class, () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));
    }

    @Test
    void depositFunds_ThrowsIllegalStateException_IfWalletInactive() {
        // Arrange
        wallet.setWalletStatus(HoldStatus.FROZEN);
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));
    }

    // Deposit Exception Handling
    @Test
    void depositFunds_ThrowsDuplicateTransactionException_OnDataIntegrityViolation() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate transaction ID", new Throwable("duplicate"));
        when(walletRepository.save(any(Wallet.class))).thenThrow(ex);

        // Act & Assert
        assertThrows(DuplicateTransactionException.class, () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));
    }

    @Test
    void depositFunds_ThrowsRuntimeException_OnOtherDataIntegrityViolation() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Some other DB error");
        when(walletRepository.save(any(Wallet.class))).thenThrow(ex);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));

        // Check that the error message contains the expected text
        assertTrue(thrown.getMessage().contains("Transaction failed") || 
                 thrown.getMessage().contains("database error") ||
                 thrown.getCause() == ex);
    }

    @Test
    void depositFunds_ThrowsRuntimeException_OnJpaSystemException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenThrow(new JpaSystemException(new RuntimeException("DB Timeout")));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));
        assertTrue(thrown.getMessage().contains("Error processing transaction. Please try again later."));
    }

    @Test
    void depositFunds_ThrowsRuntimeException_OnUnexpectedException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenThrow(new NullPointerException("NPE"));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> walletService.depositFunds(TEST_USER_CODE, transactionRequest));
        assertEquals(WalletConstants.INTERNAL_SERVER_ERROR, thrown.getMessage());
    }


    // --- withdrawFunds Tests ---

    @Test
    void withdrawFunds_Success() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        // Act
        var response = walletService.withdrawFunds(TEST_USER_CODE, transactionRequest);

        // Assert
        assertNotNull(response);
        assertEquals(DEFAULT_INITIAL_BALANCE - TRANSACTION_AMOUNT, wallet.getBalance());
        assertEquals(DEFAULT_INITIAL_BALANCE - TRANSACTION_AMOUNT, wallet.getAvailableBalance());
        assertEquals(TRANSACTION_AMOUNT, response.getProcessedAmount());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void withdrawFunds_ThrowsInsufficientFundsException() {
        // Arrange
        transactionRequest.amount(200.0); // More than available balance
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));

        // Act & Assert
        assertThrows(InsufficientFundsException.class, () -> walletService.withdrawFunds(TEST_USER_CODE, transactionRequest));
        verify(walletRepository, never()).save(any());
    }

    // Withdraw Validation/Setup Failures (same as deposit)
    @Test
    void withdrawFunds_ThrowsWalletNotFoundException() {
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class, () -> walletService.withdrawFunds(TEST_USER_CODE, transactionRequest));
    }

    @Test
    void withdrawFunds_ThrowsIllegalStateException_IfWalletInactive() {
        wallet.setWalletStatus(HoldStatus.FROZEN);
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));
        assertThrows(IllegalStateException.class, () -> walletService.withdrawFunds(TEST_USER_CODE, transactionRequest));
    }

    @Test
    void withdrawFunds_ThrowsRuntimeException_OnUnexpectedException() {
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> walletService.withdrawFunds(TEST_USER_CODE, transactionRequest));
        assertEquals(WalletConstants.INTERNAL_SERVER_ERROR, thrown.getMessage());
    }

    // --- getWalletBalance Tests ---

    @Test
    void getWalletBalance_Success() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));

        // Act
        Double balance = walletService.getWalletBalance(TEST_USER_CODE);

        // Assert
        assertEquals(DEFAULT_INITIAL_BALANCE, balance);
    }

    @Test
    void getWalletBalance_ThrowsWalletNotFoundException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletBalance(TEST_USER_CODE));
    }

    @Test
    void getWalletBalance_ThrowsRuntimeException_OnUnexpectedException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenThrow(new RuntimeException("Simulated error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.getWalletBalance(TEST_USER_CODE));
        assertEquals(WalletConstants.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // --- getAvailableBalance Tests ---

    @Test
    void getAvailableBalance_Success() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));

        // Act
        Double availableBalance = walletService.getAvailableBalance(TEST_USER_CODE);

        // Assert
        assertEquals(DEFAULT_INITIAL_BALANCE, availableBalance);
    }

    @Test
    void getAvailableBalance_ThrowsWalletNotFoundException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class, () -> walletService.getAvailableBalance(TEST_USER_CODE));
    }

    @Test
    void getAvailableBalance_ThrowsRuntimeException_OnUnexpectedException() {
        // Arrange
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenThrow(new RuntimeException("Simulated error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.getAvailableBalance(TEST_USER_CODE));
        assertEquals(WalletConstants.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // --- walletExists Tests ---

    @Test
    void walletExists_ReturnsTrue() {
        when(walletRepository.existsByUserCode(TEST_USER_CODE)).thenReturn(true);
        assertTrue(walletService.walletExists(TEST_USER_CODE));
    }

    @Test
    void walletExists_ReturnsFalse() {
        when(walletRepository.existsByUserCode(TEST_USER_CODE)).thenReturn(false);
        assertFalse(walletService.walletExists(TEST_USER_CODE));
    }

    @Test
    void walletExists_ThrowsRuntimeException_OnException() {
        when(walletRepository.existsByUserCode(TEST_USER_CODE)).thenThrow(new RuntimeException("DB Error"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.walletExists(TEST_USER_CODE));
        assertEquals(WalletConstants.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // --- Helper Method Edge Case Tests (for coverage) ---

    @Test
    void mapToWalletResponse_ThrowsIllegalStateException_IfUserSnapshotIsNull() {
        // Arrange: Simulate a database inconsistency where wallet exists but snapshot link is broken
        wallet.setUserSnapshot(null);

        // Act & Assert (Need to call a method that uses mapToWalletResponse, e.g., getWalletByUserCode)
        when(walletRepository.findByUserCode(TEST_USER_CODE)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalStateException.class, () -> walletService.getWalletByUserCode(TEST_USER_CODE));
    }
}