package com.central.wallet_service.service;

import com.central.wallet_service.constants.WalletConstants;
import com.central.wallet_service.dto.*;
import com.central.wallet_service.dto.adapter.request.*;
import com.central.wallet_service.exception.*;
import com.central.wallet_service.model.*;
import com.central.wallet_service.repository.WalletHoldRepository;
import com.central.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.openapitools.model.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.JpaSystemException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldServiceImplTest {

    @Mock private WalletHoldRepository holdRepository;
    @Mock private WalletRepository walletRepository;
    @Mock(name = "ioTaskExecutor") private ExecutorService ioExecutor;
    @Mock(name = "cpuTaskExecutor") private ExecutorService cpuExecutor;

    @InjectMocks
    private HoldServiceImpl holdService;

    private Wallet testWallet;
    private WalletHold testHold;
    private PlaceHoldRequestDto holdRequest;

    @BeforeEach
    void setUp() {
        // Setup Executors to run synchronously
        Answer<Object> syncRun = invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof Runnable) {
                ((Runnable) arg).run();
                return null;
            } else if (arg instanceof Supplier) {
                return ((Supplier<?>) arg).get();
            }
            return null;
        };
        lenient().doAnswer(syncRun).when(ioExecutor).execute(any(Runnable.class));
        lenient().doAnswer(syncRun).when(cpuExecutor).execute(any(Runnable.class));

        // Test Data setup
        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setBalance(1000.0);
        testWallet.setAvailableBalance(1000.0);
        testWallet.setWalletStatus(HoldStatus.ACTIVE);
        testWallet.setCurrency("USD");

        WalletUserSnapshot snapshot = new WalletUserSnapshot();
        snapshot.setUserCode("USER-123");
        testWallet.setUserSnapshot(snapshot);

        testHold = WalletHold.builder()
                .id(1L).holdId("HOLD-123").wallet(testWallet)
                .capturedAmount(100.0).status(HoldStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        holdRequest = new RestHoldRequestAdapter(new HoldRequest()
                .userCode("USER-123").amount(100.0).transactionId("TXN-123"));
    }

    // --- REQUESTED METHODS ---

    @Test
    void placeHold_ValidRequest_ReturnsHoldResponse() {
        when(walletRepository.findByUserCode("USER-123")).thenReturn(Optional.of(testWallet));
        when(holdRepository.save(any(WalletHold.class))).thenReturn(testHold);

        HoldResponseDto response = holdService.placeHold(holdRequest);

        assertNotNull(response);
        assertEquals("HOLD-123", response.getHoldId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void placeHold_DuplicateTransaction_ThrowsException() {
        when(walletRepository.findByUserCode(anyString())).thenReturn(Optional.of(testWallet));
        DataIntegrityViolationException dive = mock(DataIntegrityViolationException.class);
        when(dive.getMostSpecificCause()).thenReturn(new Throwable("duplicate key value"));
        when(holdRepository.save(any(WalletHold.class))).thenThrow(dive);

        assertThrows(RuntimeException.class, () -> holdService.placeHold(holdRequest));
    }

    @Test
    void adjustHold_ValidRequest_ReturnsUpdatedHold() {
        AdjustHoldRequestDto adjDto = new RestAdjustHoldRequestAdapter(new AdjustHoldRequest().newAmount(150.0));
        when(holdRepository.findByHoldId("HOLD-123")).thenReturn(Optional.of(testHold));
        when(walletRepository.findById(anyLong())).thenReturn(Optional.of(testWallet));
        when(holdRepository.save(any(WalletHold.class))).thenReturn(testHold);

        HoldResponseDto response = holdService.adjustHold("HOLD-123", adjDto);

        assertNotNull(response);
        // Verify balance was reduced further (1000 - (150-100) = 950)
        assertEquals(950.0, testWallet.getAvailableBalance());
    }

    // --- COVERAGE FOR EXCEPTIONS & EDGE CASES ---

    @Test
    void placeHold_InsufficientFunds_ThrowsException() {
        holdRequest = new RestHoldRequestAdapter(new HoldRequest().userCode("USER-123").amount(5000.0));
        when(walletRepository.findByUserCode("USER-123")).thenReturn(Optional.of(testWallet));

        assertThrows(InsufficientFundsException.class, () -> holdService.placeHold(holdRequest));
    }

    @Test
    void captureHoldFunds_InvalidStatus_ThrowsException() {
        testHold.setStatus(HoldStatus.RELEASED);
        when(holdRepository.findByHoldId("HOLD-123")).thenReturn(Optional.of(testHold));
        CaptureRequestDto capDto = new RestCaptureRequestAdapter(new CaptureRequest().holdId("HOLD-123"));

        assertThrows(IllegalStateException.class, () -> holdService.captureHoldFunds(capDto));
    }

    @Test
    void releaseHold_WalletNotFound_ThrowsException() {
        testHold.setWallet(null); // Trigger null check
        when(holdRepository.findByHoldId("HOLD-123")).thenReturn(Optional.of(testHold));
        ReleaseHoldRequestDto relDto = new RestReleaseHoldRequestAdapter(
                new ReleaseHoldRequest().reason("Test release reason"));  // Added release reason here

        assertThrows(IllegalStateException.class, () ->
                holdService.releaseHold("HOLD-123", relDto));
    }

    @Test
    void extendHold_PastDate_ThrowsException() {
        ExtendHoldRequestDto extDto = new RestExtendHoldRequestAdapter(
                new ExtendHoldRequest().newExpiresAt(OffsetDateTime.now().minusDays(1)));
        when(holdRepository.findByHoldId("HOLD-123")).thenReturn(Optional.of(testHold));

        assertThrows(IllegalArgumentException.class, () -> holdService.extendHold("HOLD-123", extDto));
    }

    @Test
    void getHoldById_NotFound_ThrowsException() {
        when(holdRepository.findByHoldId("UNKNOWN")).thenReturn(Optional.empty());
        assertThrows(HoldNotFoundException.class, () -> holdService.getHold("UNKNOWN"));
    }

    @Test
    void listHolds_HandlesNullFilters() {
        Page<WalletHold> page = new PageImpl<>(Collections.singletonList(testHold));
        when(holdRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<HoldResponseDto> result = holdService.listHolds(null, null, null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getTotalHeldAmount_HandlesException() {
        when(holdRepository.sumPendingHoldsByWalletId(anyLong())).thenThrow(new RuntimeException("DB Error"));
        Double total = holdService.getTotalHeldAmount(1L);
        assertEquals(0.0, total); // Verifies catch block returns 0.0
    }

    @Test
    void validateHold_UnexpectedException_ReturnsFalse() {
        // Force an exception inside private validation flow via mock
        when(walletRepository.findByUserCode(anyString())).thenThrow(new JpaSystemException(new RuntimeException()));

        // This hits the "Unexpected error in validateHold" catch block
        assertThrows(RuntimeException.class, () -> holdService.placeHold(holdRequest));
    }
}