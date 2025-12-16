package com.central.wallet_service.utils;

import com.central.wallet_service.dto.HoldResponseDto;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.central.wallet_service.dto.adapter.request.RestHoldRequestAdapter;
import com.central.wallet_service.dto.adapter.response.HoldResponseAdapter;
import com.central.wallet_service.dto.adapter.response.WalletResponseAdapter;
import com.central.wallet_service.dto.adapter.response.WalletTransactionResponseAdapter;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.WalletHold;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletUserSnapshot;
import org.openapitools.model.WalletTransactionResponse.TransactionTypeEnum;
import org.openapitools.model.WalletTransactionResponse.StatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.HoldResponse;

// Removed BigDecimal import as we're using double for monetary values
import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ServiceUtilsTest {

    @Test
    void validateDateRange_ValidRange_NoExceptionThrown() {
        // Arrange
        OffsetDateTime fromDate = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime toDate = OffsetDateTime.of(2023, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);

        // Act & Assert
        assertDoesNotThrow(() -> ServiceUtils.validateDateRange(fromDate, toDate));
    }

    @Test
    void validateDateRange_InvalidRange_ThrowsException() {
        // Arrange
        OffsetDateTime fromDate = OffsetDateTime.of(2023, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime toDate = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServiceUtils.validateDateRange(fromDate, toDate)
        );
        assertEquals("From date must be before or equal to To date", exception.getMessage());
    }

    @Test
    void toOffsetDateTime_ValidLocalDateTime_ReturnsOffsetDateTime() {
        // Arrange
        LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

        // Act
        OffsetDateTime result = ServiceUtils.toOffsetDateTime(localDateTime);

        // Assert
        assertNotNull(result);
        assertEquals(localDateTime.getYear(), result.getYear());
        assertEquals(localDateTime.getMonthValue(), result.getMonthValue());
        assertEquals(localDateTime.getDayOfMonth(), result.getDayOfMonth());
        assertEquals(localDateTime.getHour(), result.getHour());
        assertEquals(localDateTime.getMinute(), result.getMinute());
    }

    @Test
    void toLocalDateTime_ValidOffsetDateTime_ReturnsLocalDateTime() {
        // Arrange
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        // Act
        LocalDateTime result = ServiceUtils.toLocalDateTime(offsetDateTime);

        // Assert
        assertNotNull(result);
        assertEquals(offsetDateTime.getYear(), result.getYear());
        assertEquals(offsetDateTime.getMonthValue(), result.getMonthValue());
        assertEquals(offsetDateTime.getDayOfMonth(), result.getDayOfMonth());
    }

    @Test
    void toLocalDateTime_NullInput_ReturnsNull() {
        // Act
        LocalDateTime result = ServiceUtils.toLocalDateTime((OffsetDateTime) null);

        // Assert
        assertNull(result);
    }

    @Test
    void atEndOfDay_ValidDateTime_ReturnsEndOfDay() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2023, 1, 1, 12, 30, 0);

        // Act
        LocalDateTime result = ServiceUtils.atEndOfDay(dateTime);

        // Assert
        assertNotNull(result);
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
        assertEquals(999999999, result.getNano());
    }

    @Test
    void atEndOfDay_NullInput_ReturnsNull() {
        // Act
        LocalDateTime result = ServiceUtils.atEndOfDay(null);

        // Assert
        assertNull(result);
    }

    @Test
    void toLocalDateTime_FromInstant_ReturnsLocalDateTime() {
        // Arrange
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");

        // Act
        LocalDateTime result = ServiceUtils.toLocalDateTime(instant);

        // Assert
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void generateHoldReference_GeneratesValidReference() {
        // Act
        String reference = ServiceUtils.generateHoldReference();

        // Assert
        assertNotNull(reference);
        assertTrue(reference.startsWith("HOLD-"));
        assertEquals(13, reference.length()); // HOLD- + 8 chars of UUID
    }

    @Test
    void validateCurrency_MatchingCurrencies_NoExceptionThrown() {
        // Act & Assert
        assertDoesNotThrow(() -> ServiceUtils.validateCurrency("USD", "USD"));
    }

    @Test
    void validateCurrency_NonMatchingCurrencies_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServiceUtils.validateCurrency("USD", "EUR")
        );
        assertEquals("Currency mismatch. Expected: USD, Actual: EUR", exception.getMessage());
    }

    @Test
    void validateCurrency_NullExpected_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServiceUtils.validateCurrency(null, "USD")
        );
        assertTrue(exception.getMessage().contains("Currency mismatch"));
    }

    @Test
    void isValidStatusTransition_ValidTransitions_ReturnsTrue() {
        // Active to any status except itself is valid
        assertTrue(ServiceUtils.isValidStatusTransition(HoldStatus.ACTIVE, HoldStatus.FROZEN));
        assertTrue(ServiceUtils.isValidStatusTransition(HoldStatus.ACTIVE, HoldStatus.EXPIRED));
        assertTrue(ServiceUtils.isValidStatusTransition(HoldStatus.ACTIVE, HoldStatus.CAPTURED));
        assertTrue(ServiceUtils.isValidStatusTransition(HoldStatus.ACTIVE, HoldStatus.RELEASED));

        // Frozen can only go back to Active
        assertTrue(ServiceUtils.isValidStatusTransition(HoldStatus.FROZEN, HoldStatus.ACTIVE));

        // Same status is always valid
        assertTrue(ServiceUtils.isValidStatusTransition(HoldStatus.ACTIVE, HoldStatus.ACTIVE));
        assertTrue(ServiceUtils.isValidStatusTransition(HoldStatus.FROZEN, HoldStatus.FROZEN));
    }

    @Test
    void isValidStatusTransition_InvalidTransitions_ReturnsFalse() {
        // Frozen can only go to Active, not to other statuses
        assertFalse(ServiceUtils.isValidStatusTransition(HoldStatus.FROZEN, HoldStatus.EXPIRED));
        assertFalse(ServiceUtils.isValidStatusTransition(HoldStatus.FROZEN, HoldStatus.CAPTURED));

        // Terminal states cannot transition to any other state
        assertFalse(ServiceUtils.isValidStatusTransition(HoldStatus.EXPIRED, HoldStatus.ACTIVE));
        assertFalse(ServiceUtils.isValidStatusTransition(HoldStatus.CAPTURED, HoldStatus.ACTIVE));
        assertFalse(ServiceUtils.isValidStatusTransition(HoldStatus.RELEASED, HoldStatus.ACTIVE));
    }
    
    @Test
    void toHoldResponse_NullInput_ReturnsNull() {
        assertNull(ServiceUtils.toHoldResponse(null));
    }
    @Test
    void toHoldResponse_ValidInput_ReturnsMappedResponse() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        WalletHold walletHold = new WalletHold();
        walletHold.setHoldId("hold123");
        walletHold.setTransaction_id("txn123");
        walletHold.setOriginalAmount(100.50);
        walletHold.setRemainingAmount(100.50);
        walletHold.setCapturedAmount(0.0);
        walletHold.setStatus(HoldStatus.ACTIVE);
        walletHold.setDescription("Test hold");
        walletHold.setCreatedAt(now.toLocalDateTime());
        walletHold.setUpdatedAt(now.toLocalDateTime());
        walletHold.setExpiresAt(now.plusDays(1).toLocalDateTime());
        
        Wallet wallet = new Wallet();
        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
        userSnapshot.setUserCode("user123");
        wallet.setUserSnapshot(userSnapshot);
        walletHold.setWallet(wallet);
        
        HoldResponseDto dto = new HoldResponseAdapter(walletHold);
        
        // Act
        var result = ServiceUtils.toHoldResponse(dto);
        
        // Assert
        assertNotNull(result);
        assertEquals("hold123", result.getHoldId());
        assertEquals("user123", result.getUserCode());
        assertEquals(HoldResponse.StatusEnum.ACTIVE, result.getStatus());
        assertEquals(100.50, result.getOriginalAmount(), 0.001);
        assertEquals(100.50, result.getRemainingAmount(), 0.001);
        assertEquals(0.0, result.getCapturedAmount(), 0.001);
        assertEquals(now, result.getCreatedAt());
        assertEquals(now, result.getUpdatedAt());
        assertEquals(now.plusDays(1), result.getExpiresAt());
        assertEquals("Test hold", result.getDescription());
    }
    
    @Test
    void toWalletResponse_ValidInput_ReturnsMappedResponse() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setBalance(500.00);
        wallet.setAvailableBalance(500.00);
        wallet.setCurrency("USD");
        wallet.setWalletStatus(HoldStatus.ACTIVE);
        wallet.setCreatedAt(now.toLocalDateTime());
        
        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
        userSnapshot.setUserCode("user123");
        userSnapshot.setUsername("testuser");
        userSnapshot.setEmail("test@example.com");
        userSnapshot.setPhoneNumber("1234567890");
        wallet.setUserSnapshot(userSnapshot);
        
        WalletResponseDto dto = new WalletResponseAdapter(wallet);
        
        // Act
        var result = ServiceUtils.toWalletResponse(dto);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getWalletId());
        assertEquals("user123", result.getUserCode());
        assertEquals(500.00, result.getBalance(), 0.001);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("USD", result.getCurrency());
        assertEquals(500.00, result.getAvailableBalance(), 0.001);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("1234567890", result.getPhoneNumber());
    }

    @Test
    void toWalletTransactionResponse_ValidInput_ReturnsMappedResponse() {
        // Arrange
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setBalance(1000.0);
        wallet.setAvailableBalance(900.0);
        wallet.setCurrency("USD");
        wallet.setWalletStatus(HoldStatus.ACTIVE);
        
        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
        userSnapshot.setUserCode("user123");
        userSnapshot.setUsername("testuser");
        userSnapshot.setEmail("test@example.com");
        userSnapshot.setPhoneNumber("1234567890");
        wallet.setUserSnapshot(userSnapshot);

        WalletTransactionResponseDto dto = new WalletTransactionResponseAdapter(
                wallet,
                100.00,
                "DEPOSIT"
        );

        // Act
        var result = ServiceUtils.toWalletTransactionResponse(dto);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getWalletId());
        assertEquals(TransactionTypeEnum.DEPOSIT, result.getTransactionType());
        assertEquals(100.00, result.getProcessedAmount(), 0.001);
        assertEquals("user123", result.getUserCode());
        assertEquals(StatusEnum.COMPLETED, result.getStatus());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("1234567890", result.getPhoneNumber());
    }
    
    @Test
    void convertToHoldResponseGRPC_ValidInput_ReturnsMappedResponse() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        WalletHold walletHold = new WalletHold();
        walletHold.setHoldId("hold123");
        walletHold.setTransaction_id("txn123");
        walletHold.setOriginalAmount(100.50);
        walletHold.setRemainingAmount(100.50);
        walletHold.setCapturedAmount(0.0);
        walletHold.setStatus(HoldStatus.ACTIVE);
        walletHold.setDescription("Test hold");
        walletHold.setCreatedAt(now.toLocalDateTime());
        walletHold.setUpdatedAt(now.toLocalDateTime());
        walletHold.setExpiresAt(now.plusDays(1).toLocalDateTime());
        
        Wallet wallet = new Wallet();
        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
        userSnapshot.setUserCode("user123");
        wallet.setUserSnapshot(userSnapshot);
        walletHold.setWallet(wallet);
        
        HoldResponseDto dto = new HoldResponseAdapter(walletHold);
        
        // Act
        var result = ServiceUtils.convertToHoldResponseGRPC(dto);
        
        // Assert
        assertNotNull(result);
        assertEquals("hold123", result.getHoldId());
        assertEquals("txn123", result.getTransactionId());
        assertEquals("user123", result.getUserCode());
        assertEquals("ACTIVE", result.getStatus().name());
        assertEquals(100.50, result.getOriginalAmount(), 0.001);
        assertEquals(100.50, result.getRemainingAmount(), 0.001);
        assertEquals(0.0, result.getCapturedAmount(), 0.001);
        assertEquals("Test hold", result.getDescription());
    }
}
