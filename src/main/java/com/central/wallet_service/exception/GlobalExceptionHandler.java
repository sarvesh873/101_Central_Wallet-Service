package com.central.wallet_service.exception;

import org.openapitools.model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for the application.
 * Centralizes exception handling across all @Controller components.
 * Converts exceptions into appropriate HTTP responses with standardized error formats.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors and invalid input parameters.
     *
     * @param ex the caught IllegalArgumentException
     * @return ResponseEntity with HTTP 400 Bad Request status and error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        Double errorCode = 400.01;
        String description = "Invalid Input please check the input parameters";
        String errorType = HttpStatus.BAD_REQUEST.getReasonPhrase();
        String errorMessage = ex.getMessage();
        return generateErrorResponse(errorCode, description, errorType, errorMessage, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles database integrity violations (e.g., unique constraint violations).
     *
     * @param ex the caught DataIntegrityViolationException
     * @return ResponseEntity with HTTP 409 Conflict status and error message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        Double errorCode = 400.02;
        String description = "Data integrity violation";
        String errorType = HttpStatus.CONFLICT.getReasonPhrase();
        String errorMessage = ex.getMostSpecificCause().getMessage();
        return generateErrorResponse(errorCode, description, errorType, errorMessage, HttpStatus.CONFLICT);
    }
    
    /**
     * Handles cases when a wallet is not found.
     *
     * @param ex the caught WalletNotFoundException
     * @return ResponseEntity with HTTP 404 Not Found status and error message
     */
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFound(WalletNotFoundException ex) {
        Double errorCode = 404.01;
        String description = "Requested wallet not found";
        String errorType = HttpStatus.NOT_FOUND.getReasonPhrase();
        return generateErrorResponse(errorCode, description, errorType, ex.getMessage(), HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handles cases when a hold is not found.
     *
     * @param ex the caught HoldNotFoundException
     * @return ResponseEntity with HTTP 404 Not Found status and error message
     */
    @ExceptionHandler(HoldNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHoldNotFound(HoldNotFoundException ex) {
        Double errorCode = 404.02;
        String description = "Requested hold not found";
        String errorType = HttpStatus.NOT_FOUND.getReasonPhrase();
        return generateErrorResponse(errorCode, description, errorType, ex.getMessage(), HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handles cases when there are insufficient funds for a transaction.
     *
     * @param ex the caught InsufficientFundsException
     * @return ResponseEntity with HTTP 400 Bad Request status and error message
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        Double errorCode = 400.03;
        String description = "Insufficient funds for the transaction";
        String errorType = HttpStatus.BAD_REQUEST.getReasonPhrase();
        return generateErrorResponse(errorCode, description, errorType, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles validation errors for method arguments.
     *
     * @param ex the caught MethodArgumentNotValidException
     * @return ResponseEntity with HTTP 400 Bad Request status and validation error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Double errorCode = 400.04;
        String description = "Validation error";
        String errorType = HttpStatus.BAD_REQUEST.getReasonPhrase();
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return generateErrorResponse(errorCode, description, errorType, errorMessage, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles type mismatch in method arguments.
     *
     * @param ex the caught MethodArgumentTypeMismatchException
     * @return ResponseEntity with HTTP 400 Bad Request status and error message
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleTypeMismatch(Exception ex) {
        Double errorCode = 400.05;
        String description = "Invalid parameter type or missing required parameter";
        String errorType = HttpStatus.BAD_REQUEST.getReasonPhrase();
        return generateErrorResponse(errorCode, description, errorType, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles all other uncaught exceptions.
     *
     * @param ex the caught Exception
     * @return ResponseEntity with HTTP 500 Internal Server Error status and error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        Double errorCode = 500.00;
        String description = "An unexpected error occurred";
        String errorType = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "No error message available";
        return generateErrorResponse(errorCode, description, errorType, errorMessage, 
                                   HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper method to generate a standardized error response.
     *
     * @param errorCode    Application-specific error code
     * @param description  Human-readable description of the error
     * @param errorType    Type/category of the error
     * @param errorMessage Detailed error message
     * @param status       HTTP status code
     * @return ResponseEntity containing the error response
     */
    private ResponseEntity<ErrorResponse> generateErrorResponse(
            Double errorCode, String description, String errorType, String errorMessage, HttpStatus status) {
        ErrorResponse errorResponse = new ErrorResponse()
                .errorCode(errorCode)
                .description(description)
                .errorType(errorType)
                .errorMessage(errorMessage);
        
        return new ResponseEntity<>(errorResponse, status);
    }
}
