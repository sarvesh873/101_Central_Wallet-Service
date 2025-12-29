package com.central.wallet_service.utils;


import com.central.wallet.HoldResponseGRPC;
import com.central.wallet_service.dto.HoldResponseDto;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.WalletHold;
import com.google.protobuf.Timestamp;
import org.openapitools.model.HoldResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.model.WalletResponse;
import org.openapitools.model.WalletTransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
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
     * @throws IllegalArgumentException if the date range is invalid
     */
    public static void validateDateRange(OffsetDateTime fromDate, OffsetDateTime toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date must be before or equal to To date");
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
     * Generates a unique transaction reference
     * @return A unique transaction reference string
     */
    public static String generateHoldReference() {
        return "HOLD" + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Validates that the target currency matches the expected currency
     * @param expected The expected currency code
     * @param actual The actual currency code to validate
     * @throws IllegalArgumentException if currencies don't match
     */
    public static void validateCurrency(String expected, String actual) {
        if (expected == null || !expected.equals(actual)) {
            throw new IllegalArgumentException("Currency mismatch. Expected: " + expected + ", Actual: " + actual);
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

    public static HoldResponse toHoldResponse(HoldResponseDto dto) {
        if (dto == null) {
            return null;
        }

        return new HoldResponse()
                .holdId(dto.getHoldId())
                .userCode(dto.getUserCode())
                .status(HoldResponse.StatusEnum.fromValue(dto.getStatus()))
                .originalAmount(dto.getOriginalAmount())
                .remainingAmount(dto.getRemainingAmount())
                .capturedAmount(dto.getCapturedAmount())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .expiresAt(dto.getExpiresAt())
                .description(dto.getDescription());
    }

    public static WalletResponse toWalletResponse(WalletResponseDto responseDto) {
        return new WalletResponse()
                .walletId(responseDto.getWalletId())
                .userCode(responseDto.getUserCode())
                .balance(responseDto.getBalance())
                .status(responseDto.getStatus())
                .currency(responseDto.getCurrency())
                .availableBalance(responseDto.getAvailableBalance())
                .createdAt(responseDto.getCreatedAt())
                .username(responseDto.getUsername())
                .email(responseDto.getEmail())
                .phoneNumber(responseDto.getPhoneNumber());
    }

    public static WalletTransactionResponse toWalletTransactionResponse(WalletTransactionResponseDto dto) {
        return new WalletTransactionResponse()
                .walletId(dto.getWalletId())
                .transactionType(WalletTransactionResponse.TransactionTypeEnum.fromValue(dto.getTransactionType().name()))
                .processedAmount(dto.getProcessedAmount())
                .newBalance(dto.getNewBalance())
                .newAvailableBalance(dto.getNewAvailableBalance())
                .userCode(dto.getUserCode())
                .status(WalletTransactionResponse.StatusEnum.fromValue(dto.getStatus().name()))
                .username(dto.getUsername())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber());
    }

    // Helper methods Grpc
    public static HoldResponseGRPC convertToHoldResponseGRPC(HoldResponseDto response) {
        if (response == null) {
            return null;
        }

        return HoldResponseGRPC.newBuilder()
                .setHoldId(response.getHoldId())
                .setTransactionId(response.getTransaction_id())
                .setUserCode(response.getUserCode())
                .setStatus(HoldResponseGRPC.HoldStatusGRPC.valueOf(response.getStatus()))
                .setOriginalAmount(response.getOriginalAmount())
                .setRemainingAmount(response.getRemainingAmount())
                .setCapturedAmount(response.getCapturedAmount())
                .setCreatedAt(convertToTimestamp(response.getCreatedAt()))
                .setExpiresAt(convertToTimestamp(response.getExpiresAt()))
                .setUpdatedAt(convertToTimestamp(response.getUpdatedAt()))
                .setDescription(response.getDescription())
                .build();
    }

    public static Timestamp convertToTimestamp(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        Instant instant = offsetDateTime.toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneOffset.UTC
        );
    }

}

