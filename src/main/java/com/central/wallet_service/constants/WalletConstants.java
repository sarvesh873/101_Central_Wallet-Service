package com.central.wallet_service.constants;

public final class WalletConstants {
    private WalletConstants() {
        throw new IllegalStateException("Utility class");
    }

    // Common
    public static final String INTERNAL_SERVER_ERROR = "Transaction processing failed";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String OPERATION_NOT_ALLOWED = "Operation not allowed";
    
    // Wallet related
    public static final String WALLET_ALREADY_EXISTS = "Wallet already exists for user code: %s";
    public static final String WALLET_NOT_FOUND = "Wallet not found for user code: %s";
    public static final String WALLET_INACTIVE = "Wallet is not active for user code: %s";
    public static final String WALLET_SUSPENDED = "Wallet is suspended for user code: %s";
    
    // Transaction related
    public static final String INVALID_AMOUNT = "Amount must be greater than zero";

    
    // Hold related
    public static final String HOLD_NOT_FOUND = "Hold not found with ID: %s";
    public static final String HOLD_EXPIRED = "Hold has expired";
    public static final String HOLD_ALREADY_PROCESSED = "Hold has already been %s";
    public static final String INVALID_HOLD_STATE = "Invalid hold state for operation";
    public static final String HOLD_AMOUNT_MISMATCH = "Hold amount does not match the requested amount";
    public static final String HOLD_VALIDATION_FAILED = "Hold validation failed";
    
    // Validation messages
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String INVALID_USER_CODE = "Invalid or missing user code";
    public static final String INVALID_CURRENCY = "Invalid or missing currency";
    public static final String INVALID_REFERENCE = "Invalid reference ID";
    public static final String INVALID_DATE = "Invalid date format or value";
    public static final String INVALID_STATUS = "Invalid status provided";
    
    // Transaction types
    public static final String TRANSACTION_TYPE_DEPOSIT = "DEPOSIT";
    public static final String TRANSACTION_TYPE_WITHDRAWAL = "WITHDRAWAL";
    public static final String TRANSACTION_FAILED = "Transaction processing failed";
    
    // Status messages
    public static final String WALLET_CREATED = "Wallet created successfully";
    public static final String DEPOSIT_SUCCESSFUL = "Deposit successful";
    public static final String WITHDRAWAL_SUCCESSFUL = "Withdrawal successful";
    public static final String TRANSACTION_COMPLETED = "Transaction completed successfully";
    public static final String HOLD_PLACED = "Hold placed successfully";
    public static final String HOLD_CAPTURED = "Hold captured successfully";
    public static final String HOLD_RELEASED = "Hold released successfully";
    public static final String HOLD_EXTENDED = "Hold extended successfully";
    public static final String HOLD_ADJUSTED = "Hold adjusted successfully";
    
    // Log messages
    public static final String LOG_WALLET_CREATED = "Created new wallet for user: {}";
    public static final String LOG_WALLET_UPDATED = "Updated wallet for user: {}";
    public static final String LOG_WALLET_STATUS_CHANGED = "Changed status of wallet by {} from {} to {}";
    public static final String LOG_DEPOSIT = "Deposited {} {} to wallet: {}";
    public static final String LOG_WITHDRAWAL = "Withdrew {} {} from wallet: {}";
    public static final String LOG_TRANSACTION_VALIDATION_FAILED = "Transaction validation failed for wallet: {}";
    public static final String LOG_HOLD_PLACED = "Placed hold of {} {} on wallet: {}";
    public static final String LOG_HOLD_CAPTURED = "Captured {} {} from hold: {}";
    public static final String LOG_HOLD_RELEASED = "Released hold: {}";
    public static final String LOG_HOLD_EXTENDED = "Extended hold: {} until {}";
    public static final String LOG_HOLD_ADJUSTED = "Adjusted hold: {} to {}";
    public static final String LOG_HOLD_VALIDATION_FAILED = "Hold validation failed: {}";
    
    // Default values
    public static final String DEFAULT_CURRENCY = "INR";
    public static final double DEFAULT_INITIAL_BALANCE = 0.0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    public static final String INSUFFICIENT_FUNDS = "Insufficient funds in wallet for user:{}. Current balance:{}, Required:{}";
}
