package com.central.wallet_service.service;

import com.central.wallet_service.constants.WalletConstants;
import com.central.wallet_service.exception.DuplicateTransactionException;
import com.central.wallet_service.exception.InsufficientFundsException;
import com.central.wallet_service.exception.WalletNotFoundException;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletUserSnapshot;
import com.central.wallet_service.repository.WalletRepository;
import com.central.wallet_service.repository.WalletUserSnapshotRepository;
import com.central.wallet_service.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.central.wallet_service.utils.ServiceUtils.isValidStatusTransition;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletServiceImpl implements WalletService {

    @Autowired
    private final WalletRepository walletRepository;

    @Autowired
    private final WalletUserSnapshotRepository userSnapshotRepository;


    @Override
    @Transactional
    public WalletResponse createWallet(WalletCreateRequest request) {
        try {
            validateWalletRequest(request);
            log.info("Creating wallet for user: {} with {}", request.getUserCode(), request);
            
            if (walletExists(request.getUserCode())) {
                throw new IllegalStateException(
                    String.format(WalletConstants.WALLET_ALREADY_EXISTS, request.getUserCode()));
            }
            // First, save the user snapshot
            WalletUserSnapshot userSnapshot = WalletUserSnapshot.builder()
                    .userCode(request.getUserCode())
                    .build();
            
            // Save the user snapshot first
            userSnapshot = userSnapshotRepository.save(userSnapshot);
            
            // Then create and save the wallet with the persisted user snapshot
            Wallet wallet = Wallet.builder()
                    .userSnapshot(userSnapshot)
                    .balance(WalletConstants.DEFAULT_INITIAL_BALANCE)
                    .availableBalance(WalletConstants.DEFAULT_INITIAL_BALANCE)
                    .currency(request.getCurrency() != null ? 
                        request.getCurrency() : WalletConstants.DEFAULT_CURRENCY)
                    .walletStatus(HoldStatus.ACTIVE)
                    .lastModifiedBy(request.getUserCode())
                    .build();
                
            Wallet savedWallet = walletRepository.save(wallet);

            log.info(WalletConstants.LOG_WALLET_CREATED, request.getUserCode());
            return mapToWalletResponse(savedWallet);
            
        } catch (DataIntegrityViolationException ex) {
            String errorMsg = "Error creating wallet - database constraint violation: " + ex.getMostSpecificCause().getMessage();
            log.error(errorMsg, ex);
            if (ex.getMostSpecificCause().getMessage().toLowerCase().contains("duplicate")) {
                throw new DuplicateTransactionException("A wallet with this user code already exists");
            }
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR, ex);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.error("Validation error creating wallet: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating wallet: {}", ex.getMessage(), ex);
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR, ex);
        }
    }

    @Override
    public WalletResponse getWalletByUserCode(String userCode) {
        try {
            if (StringUtils.isBlank(userCode)) {
                throw new IllegalArgumentException(WalletConstants.INVALID_USER_CODE);
            }
            
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new WalletNotFoundException(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
                    
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                throw new IllegalStateException(
                    String.format(WalletConstants.WALLET_INACTIVE, userCode));
            }
            
            return mapToWalletResponse(wallet);
            
        } catch (IllegalArgumentException | IllegalStateException | WalletNotFoundException ex) {
            log.error("Error retrieving wallet: {}", ex.getMessage(), ex);
            throw ex;
        } catch (JpaSystemException ex) {
            log.error("Database error retrieving wallet: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error accessing wallet data. Please try again later.", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving wallet: {}", ex.getMessage(), ex);
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR, ex);
        }
    }

    @Override
    @Transactional
    public WalletTransactionResponse depositFunds(String userCode, WalletTransactionRequest request) {
        try {
            validateTransactionRequest(userCode, request);
            
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new WalletNotFoundException(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
                    
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                throw new IllegalStateException(
                    String.format(WalletConstants.WALLET_INACTIVE, userCode));
            }

            double amount = request.getAmount();
            double newBalance = wallet.getBalance() + amount;
            double newAvailableBalance = wallet.getAvailableBalance() + amount;

            wallet.setBalance(newBalance);
            wallet.setAvailableBalance(newAvailableBalance);
            wallet.setUpdatedAt(LocalDateTime.now());
            wallet.setLastModifiedBy(userCode);
            
            Wallet updatedWallet = walletRepository.save(wallet);
            
            log.info(WalletConstants.DEPOSIT_SUCCESSFUL + " for user: {}", userCode);
            return createTransactionResponse(updatedWallet, amount, WalletConstants.TRANSACTION_TYPE_DEPOSIT);
            
        } catch (DataIntegrityViolationException ex) {
            String errorMsg = "Transaction failed - database error: " + ex.getMostSpecificCause().getMessage();
            log.error("{} for user {}: {}", WalletConstants.TRANSACTION_FAILED, userCode, errorMsg, ex);
            if (ex.getMostSpecificCause().getMessage().toLowerCase().contains("duplicate")) {
                throw new DuplicateTransactionException("A transaction with this ID already exists");
            }
            throw new RuntimeException("Transaction failed due to a database error. Please try again.", ex);
        } catch (IllegalArgumentException | IllegalStateException | WalletNotFoundException ex) {
            log.error("{} for user {}: {}", WalletConstants.TRANSACTION_FAILED, userCode, ex.getMessage(), ex);
            throw ex;
        } catch (JpaSystemException ex) {
            log.error("Database error processing transaction for user {}: {}", userCode, ex.getMessage(), ex);
            throw new RuntimeException("Error processing transaction. Please try again later.", ex);
        } catch (Exception ex) {
            log.error("Unexpected error processing transaction for user {}: {}", userCode, ex.getMessage(), ex);
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR, ex);
        }
    }

    @Override
    @Transactional
    public WalletTransactionResponse withdrawFunds(String userCode, WalletTransactionRequest request) {
        try {
            validateTransactionRequest(userCode, request);
            
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new WalletNotFoundException(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
            
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                throw new IllegalStateException(
                    String.format(WalletConstants.WALLET_INACTIVE, userCode));
            }
            
            double amount = request.getAmount().doubleValue();
            if (wallet.getAvailableBalance() < amount) {
                throw new InsufficientFundsException(
                    String.format("Insufficient funds. Available: %.2f, Required: %.2f",
                        wallet.getAvailableBalance(),
                        amount
                    )
                );
            }

            double newBalance = wallet.getBalance() - amount;
            double newAvailableBalance = wallet.getAvailableBalance() - amount;

            wallet.setBalance(newBalance);
            wallet.setAvailableBalance(newAvailableBalance);
            wallet.setUpdatedAt(LocalDateTime.now());
            wallet.setLastModifiedBy(userCode);
            
            Wallet updatedWallet = walletRepository.save(wallet);
            
            return createTransactionResponse(updatedWallet, amount, "WITHDRAWAL");
            
        } catch (IllegalArgumentException | IllegalStateException | WalletNotFoundException | InsufficientFundsException ex) {
            log.error("Error processing withdrawal for user {}: {}", userCode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing withdrawal for user {}: {}", userCode, ex.getMessage(), ex);
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    public Double getWalletBalance(String userCode) {
        try {
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new WalletNotFoundException(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));

            return wallet.getBalance();

        } catch (WalletNotFoundException ex) {
            log.error("Error getting balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error getting balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Double getAvailableBalance(String userCode) {
        try {
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new WalletNotFoundException(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
            
            return wallet.getAvailableBalance();
            
        } catch (WalletNotFoundException ex) {
            log.error("Error getting available balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error getting available balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public boolean walletExists(String userCode) {
        try {
            return walletRepository.existsByUserCode(userCode);
        } catch (Exception ex) {
            log.error("Error checking if wallet exists for user {}: {}", userCode, ex.getMessage(), ex);
            throw new RuntimeException(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }


    // Helper Methods
    
    private void validateWalletRequest(WalletCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(WalletConstants.INVALID_REQUEST);
        }
        
        if (StringUtils.isBlank(request.getUserCode())) {
            throw new IllegalArgumentException(WalletConstants.INVALID_USER_CODE);
        }
        
        if (request.getCurrency() == null) {
            throw new IllegalArgumentException(WalletConstants.INVALID_CURRENCY);
        }
    }
    
    private void validateTransactionRequest(String userCode, WalletTransactionRequest request) {
        if (StringUtils.isBlank(userCode)) {
            throw new IllegalArgumentException(WalletConstants.INVALID_USER_CODE);
        }
        
        if (request == null) {
            throw new IllegalArgumentException(WalletConstants.INVALID_REQUEST);
        }
        
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException(WalletConstants.INVALID_AMOUNT);
        }

    }
    
    private WalletResponse mapToWalletResponse(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        
        WalletUserSnapshot userSnapshot = wallet.getUserSnapshot();
        if (userSnapshot == null) {
            throw new IllegalStateException("User snapshot not found for wallet with ID: " + wallet.getId());
        }
            
        return new WalletResponse()
            .walletId(wallet.getId())
            .userCode(userSnapshot.getUserCode())
            .username(userSnapshot.getUsername())
            .email(userSnapshot.getEmail())
            .phoneNumber(userSnapshot.getPhoneNumber())
            .balance(wallet.getBalance())
            .availableBalance(wallet.getAvailableBalance())
            .currency(wallet.getCurrency())
            .status(wallet.getWalletStatus().name())
            .createdAt(ServiceUtils.toOffsetDateTime(wallet.getCreatedAt()));
    }
    
    private WalletTransactionResponse createTransactionResponse(Wallet wallet, double amount, String type) {
        if (wallet == null) {
            return null;
        }

        WalletUserSnapshot userSnapshot = wallet.getUserSnapshot();
        if (userSnapshot == null) {
            throw new IllegalStateException("User snapshot not found for wallet with ID: " + wallet.getId());
        }

        return new WalletTransactionResponse()
                .walletId(wallet.getId())
                .transactionType(WalletTransactionResponse.TransactionTypeEnum.fromValue(type))
                .processedAmount(amount)
                .newBalance(wallet.getBalance())
                .newAvailableBalance(wallet.getAvailableBalance())
                .userCode(userSnapshot.getUserCode())
                .status(WalletTransactionResponse.StatusEnum.COMPLETED)
                .username(userSnapshot.getUsername())
                .email(userSnapshot.getEmail())
                .phoneNumber(userSnapshot.getPhoneNumber());
    }
}
