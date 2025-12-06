package com.central.wallet_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a wallet doesn't have sufficient funds for a transaction.
 * Results in a 400 BAD_REQUEST response when handled by the global exception handler.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InsufficientFundsException extends RuntimeException {
    
    /**
     * Constructs a new InsufficientFundsException with the specified detail message.
     *
     * @param message the detail message
     */
    public InsufficientFundsException(String message) {
        super(message);
    }

    /**
     * Creates a formatted error message for insufficient funds.
     *
     * @param userCode the user code of the wallet
     * @param currentBalance the current balance of the wallet
     * @param requiredAmount the amount that was attempted to be withdrawn
     * @return a formatted error message
     */
    public static String createMessage(String userCode, Double currentBalance, Double requiredAmount) {
        return String.format("Insufficient funds in wallet for user %s. Current balance: %.2f, Required: %.2f", 
                           userCode, currentBalance, requiredAmount);
    }
}
