package com.central.wallet_service.utils;

import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.WalletHold;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletUserSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.HoldResponse;

import java.math.BigDecimal;
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
    void toOffsetDateTime_NullInput_ReturnsNull() {
        // Act
        OffsetDateTime result = ServiceUtils.toOffsetDateTime(null);
        
        // Assert
        assertNull(result);
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
    void validatePositiveAmount_ValidAmount_NoExceptionThrown() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.50");
        
        // Act & Assert
        assertDoesNotThrow(() -> ServiceUtils.validatePositiveAmount(amount));
    }

    @Test
    void validatePositiveAmount_NullAmount_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServiceUtils.validatePositiveAmount(null)
        );
        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void validatePositiveAmount_ZeroAmount_ThrowsException() {
        // Arrange
        BigDecimal amount = BigDecimal.ZERO;
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServiceUtils.validatePositiveAmount(amount)
        );
        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void validatePositiveAmount_NegativeAmount_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("-100.50");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServiceUtils.validatePositiveAmount(amount)
        );
        assertEquals("Amount must be greater than zero", exception.getMessage());
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
    void constructHoldResponse_ValidWalletHold_ReturnsHoldResponse() {
        // Arrange
        WalletUserSnapshot userSnapshot = new WalletUserSnapshot();
        userSnapshot.setUserCode("USER123");
        
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUserSnapshot(userSnapshot);
        
        LocalDateTime now = LocalDateTime.now();
        
        WalletHold hold = new WalletHold();
        hold.setHoldId("HOLD123");
        hold.setWallet(wallet);
        hold.setOriginalAmount(100.0);
        hold.setRemainingAmount(50.0);
        hold.setCapturedAmount(50.0);
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setTransaction_id("TXN123");
        hold.setDescription("Test hold");
        hold.setExpiresAt(now.plusDays(1));
        hold.setCreatedAt(now);
        hold.setUpdatedAt(now);
        
        // Act
        HoldResponse response = ServiceUtils.constructHoldResponse(hold);
        
        // Assert
        assertNotNull(response);
        assertEquals("HOLD123", response.getHoldId());
        assertEquals("USER123", response.getUserCode());
        assertEquals(100.0, response.getOriginalAmount());
        assertEquals(50.0, response.getRemainingAmount());
        assertEquals(50.0, response.getCapturedAmount());
        assertEquals(HoldResponse.StatusEnum.ACTIVE, response.getStatus());
        assertEquals("TXN123", response.getTransactionId());
        assertEquals("Test hold", response.getDescription());
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void constructHoldResponse_NullInput_ReturnsNull() {
        // Act
        HoldResponse response = ServiceUtils.constructHoldResponse(null);
        
        // Assert
        assertNull(response);
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
}
