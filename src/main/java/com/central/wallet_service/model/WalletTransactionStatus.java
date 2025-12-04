package com.central.wallet_service.model;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the status of a transaction in the system.
 * Each status indicates a specific state in the transaction lifecycle.
 */
public enum WalletTransactionStatus {
    // Success states
    COMPLETED("Completed", "Transaction was successfully completed"),
    // Final failure states
    FAILED("Failed", "Transaction failed to process"),
    DECLINED("Declined", "Transaction was declined by business rules"),
    EXPIRED("Expired", "Transaction expired before completion");

    private final String displayName;
    private final String description;

    WalletTransactionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns a user-friendly display name for the status
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a description of what this status means
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status is terminal (no further state changes expected)
     */
    public boolean isTerminal() {
        return List.of(COMPLETED, FAILED, DECLINED, EXPIRED)
                .contains(this);
    }

    /**
     * Checks if this status indicates a successful transaction
     */
    public boolean isSuccessful() {
        return this == COMPLETED ;
    }

    /**
     * Checks if this status indicates a failed or error state
     */
    public boolean isFailed() {
        return this == FAILED || this == DECLINED || this == EXPIRED;
    }


    /**
     * Gets all terminal statuses
     */
    public static List<WalletTransactionStatus> getTerminalStatuses() {
        return Arrays.stream(values())
                .filter(WalletTransactionStatus::isTerminal)
                .toList();
    }

    /**
     * Gets all non-terminal statuses
     */
    public static List<WalletTransactionStatus> getNonTerminalStatuses() {
        return Arrays.stream(values())
                .filter(status -> !status.isTerminal())
                .toList();
    }
}

