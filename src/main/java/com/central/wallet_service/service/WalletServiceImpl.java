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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.central.wallet_service.dto.WalletCreateRequestDto;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.dto.WalletTransactionRequestDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto;
import com.central.wallet_service.dto.adapter.response.WalletResponseAdapter;
import com.central.wallet_service.dto.adapter.response.WalletTransactionResponseAdapter;
import org.openapitools.model.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletUserSnapshotRepository userSnapshotRepository;

    public WalletServiceImpl(
            WalletRepository walletRepository, WalletUserSnapshotRepository userSnapshotRepository) {

        this.walletRepository = walletRepository;
        this.userSnapshotRepository = userSnapshotRepository;
    }

    @Override
    @Transactional
    public WalletResponseDto createWallet(WalletCreateRequestDto request) {
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
            return new WalletResponseAdapter(savedWallet);

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
    public WalletResponseDto getWalletByUserCode(String userCode) {
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

            log.debug("Retrieved wallet for user: {}", userCode);
            return new WalletResponseAdapter(wallet);

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
    public WalletTransactionResponseDto depositFunds(String userCode, WalletTransactionRequestDto request) {
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
            return new WalletTransactionResponseAdapter(updatedWallet, request.getAmount(), "DEPOSIT");

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
    public WalletTransactionResponseDto withdrawFunds(String userCode, WalletTransactionRequestDto request) {
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
            return new WalletTransactionResponseAdapter(updatedWallet, amount, "WITHDRAWAL");

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

    private void validateWalletRequest(WalletCreateRequestDto request) {
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

    private void validateTransactionRequest(String userCode, WalletTransactionRequestDto request) {
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
}
