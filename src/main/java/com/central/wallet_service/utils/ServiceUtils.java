package com.central.wallet_service.utils;

import com.central.wallet_service.exception.WalletException;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.WalletHold;
import org.openapitools.model.HoldResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Utility class for common service operations.
 */
@Slf4j
public final class ServiceUtils {

    private ServiceUtils() {
        throw new IllegalStateException("Utility class");
    }

    
    /**
     * Converts LocalDateTime to OffsetDateTime
     * @param localDateTime The LocalDateTime to convert
     * @return OffsetDateTime or null if input is null
     */
    /**
     * Validates a date range (fromDate must be before toDate)
     * @param fromDate Start date
     * @param toDate End date
     * @throws WalletException if the date range is invalid
     */
    public static void validateDateRange(OffsetDateTime fromDate, OffsetDateTime toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw WalletException.badRequest("From date must be before or equal to To date");
        }
    }
    
    /**
     * Converts LocalDateTime to OffsetDateTime using system default timezone
     * @param localDateTime The LocalDateTime to convert
     * @return OffsetDateTime or null if input is null
     */
    public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
    
    /**
     * Converts OffsetDateTime to LocalDateTime while preserving the instant in time
     * @param offsetDateTime The OffsetDateTime to convert
     * @return LocalDateTime representing the same instant in the system default timezone, or null if input is null
     */
    public static LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        // Convert to system default timezone while preserving the instant in time
        return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Gets the start of the day (00:00:00) for the given date
     * @param date The date to get start of day for
     * @return Start of the day as LocalDateTime
     */
    public static LocalDateTime atStartOfDay(LocalDateTime date) {
        return date != null ? date.toLocalDate().atStartOfDay() : null;
    }
    
    /**
     * Gets the end of the day (23:59:59.999999999) for the given date
     * @param date The date to get end of day for
     * @return End of the day as LocalDateTime
     */
    public static LocalDateTime atEndOfDay(LocalDateTime date) {
        return date != null ? date.toLocalDate().atTime(23, 59, 59, 999999999) : null;
    }
    
    /**
     * Converts Instant to LocalDateTime
     * @param instant The Instant to convert
     * @return LocalDateTime or null if input is null
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
    
    /**
     * Validates if the amount is positive
     * @param amount The amount to validate
     * @throws WalletException if amount is null or not positive
     */
    public static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
    }
    
    /**
     * Validates if the string is not blank
     * @param value The string to validate
     * @param fieldName The name of the field for error message
     * @throws WalletException if value is blank
     */
    public static void validateNotBlank(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw WalletException.badRequest(String.format("%s cannot be blank", fieldName));
        }
    }
    
    /**
     * Validates if the object is not null
     * @param obj The object to check
     * @param message The error message if null
     * @param <T> The type of the object
     * @return The object if not null
     * @throws WalletException if object is null
     */
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw WalletException.badRequest(message);
        }
        return obj;
    }
    
    /**
     * Generates a unique transaction reference
     * @return A unique transaction reference string
     */
    public static String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Formats an amount with the specified number of decimal places
     * @param amount The amount to format
     * @param decimalPlaces The number of decimal places
     * @return Formatted amount
     */
    public static BigDecimal formatAmount(BigDecimal amount, int decimalPlaces) {
        requireNonNull(amount, "Amount cannot be null");
        return amount.setScale(decimalPlaces, RoundingMode.HALF_EVEN);
    }
    
    /**
     * Validates that the target currency matches the expected currency
     * @param expected The expected currency code
     * @param actual The actual currency code to validate
     * @throws WalletException if currencies don't match
     */
    public static void validateCurrency(String expected, String actual) {
        if (expected == null || !expected.equals(actual)) {
            throw WalletException.badRequest("Currency mismatch. Expected: " + expected + ", Actual: " + actual);
        }
    }



    /**
     * Maps a WalletHold entity to a HoldResponse DTO using builder pattern
     * @param hold The WalletHold entity to map
     * @return HoldResponse DTO or null if input is null
     */
    public static HoldResponse constructHoldResponse(WalletHold hold) {
        if (hold == null) {
            log.warn("Attempted to map null WalletHold to HoldResponse");
            return null;
        }
        
        try {
            String holdId = hold.getId() != null ? hold.getId().toString() : "null";
            String walletId = hold.getWallet() != null ? 
                (hold.getWallet().getId() != null ? hold.getWallet().getId().toString() : "null") : "null";
            
            log.debug("Mapping WalletHold to HoldResponse - Hold ID: {}, Wallet ID: {}", holdId, walletId);
            
            return HoldResponse.builder()
                .holdId(holdId)
                .userCode(hold.getWallet() != null && hold.getWallet().getUserSnapshot() != null ? 
                    hold.getWallet().getUserSnapshot().getUserCode() : null)
                .remainingAmount(hold.getRemainingAmount())
                .status(HoldResponse.StatusEnum.valueOf(hold.getStatus().name()))
                .transactionId(hold.getTransaction_id())
                .description(hold.getDescription())
                .expiresAt(toOffsetDateTime(hold.getExpiresAt()))
                .createdAt(toOffsetDateTime(hold.getCreatedAt()))
                .updatedAt(toOffsetDateTime(hold.getUpdatedAt()))
                .build();
                
        } catch (Exception e) {
            log.error("Error mapping WalletHold to HoldResponse: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public static boolean isValidStatusTransition(HoldStatus currentStatus, HoldStatus newStatus) {
        // If the status hasn't changed, it's always valid
        if (currentStatus == newStatus) {
            return true;
        }

        // Define valid transitions
        switch (currentStatus) {
            case ACTIVE:
                // ACTIVE can transition to any status except itself (already handled above)
                return true;

            case FROZEN:
                // FROZEN can only transition back to ACTIVE
                return newStatus == HoldStatus.ACTIVE;

            case EXPIRED:
                // EXPIRED is a terminal state, cannot transition to any other state
                return false;

            case CAPTURED:
                // CAPTURED is a terminal state for holds, cannot transition to any other state
                return false;

            case RELEASED:
                // RELEASED is a terminal state for holds, cannot transition to any other state
                return false;

            default:
                // By default, don't allow any other transitions
                return false;
        }
    }
}

