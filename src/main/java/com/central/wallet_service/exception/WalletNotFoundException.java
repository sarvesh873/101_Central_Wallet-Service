package com.central.wallet_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a wallet cannot be found for a given user code.
 * Results in a 404 NOT_FOUND response when handled by the global exception handler.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class WalletNotFoundException extends RuntimeException {
    
    /**
     * Constructs a new WalletNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public WalletNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new WalletNotFoundException with the specified user code.
     *
     * @param userCode the user code for which the wallet was not found
     * @return a formatted error message
     */
    public static String createMessage(String userCode) {
        return String.format("Wallet not found for user code: %s", userCode);
    }
}
