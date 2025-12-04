package com.central.wallet_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WalletException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final String errorMessage;
    private final String details;

    public WalletException(HttpStatus status, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.errorCode = String.valueOf(status.value());
        this.errorMessage = errorMessage;
        this.details = null;
    }

    public WalletException(HttpStatus status, String errorCode, String errorMessage, String details) {
        super(errorMessage);
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.details = details;
    }

    public static WalletException notFound(String message) {
        return new WalletException(HttpStatus.NOT_FOUND, message);
    }

    public static WalletException badRequest(String message) {
        return new WalletException(HttpStatus.BAD_REQUEST, message);
    }

    public static WalletException conflict(String message) {
        return new WalletException(HttpStatus.CONFLICT, message);
    }

    public static WalletException internalServerError(String message) {
        return new WalletException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static WalletException insufficientFunds(String message) {
        return new WalletException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", message, null);
    }

    public static WalletException validationFailed(String field, String message) {
        return new WalletException(HttpStatus.BAD_REQUEST, 
            "VALIDATION_FAILED", 
            String.format("Validation failed for field '%s'", field),
            message);
    }
}
