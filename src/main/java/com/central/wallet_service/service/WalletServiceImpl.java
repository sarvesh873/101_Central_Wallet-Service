package com.central.wallet_service.service;

import com.central.wallet_service.constants.WalletConstants;
import com.central.wallet_service.exception.WalletException;
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
    private  WalletUserSnapshotRepository userSnapshotRepository;


    @Override
    @Transactional
    public WalletResponse createWallet(WalletCreateRequest request) {
        try {
            validateWalletRequest(request);
            log.info("Creating wallet for user: {} with {}", request.getUserCode(), request);
            
            if (walletExists(request.getUserCode())) {
                throw WalletException.badRequest(
                    String.format(WalletConstants.WALLET_ALREADY_EXISTS, request.getUserCode()));
            }
            // First, save the user snapshot
            WalletUserSnapshot userSnapshot = WalletUserSnapshot.builder()
                    .userCode(request.getUserCode())
                    .username(request.getUserCode()) // You might want to set this properly
                    .email(request.getUserCode() + "@example.com") // You might want to set this properly
                    .phoneNumber("+1234567890") // You might want to set this properly
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
            
            // Update the user snapshot's wallets set if needed
//            userSnapshot.getWallets().add(savedWallet);
//            userSnapshotRepository.save(userSnapshot);
            log.info(WalletConstants.LOG_WALLET_CREATED, request.getUserCode());
            return mapToWalletResponse(savedWallet);
            
        } catch (WalletException ex) {
            log.error("Error creating wallet: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating wallet: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public WalletResponse getWalletByUserCode(String userCode) {
        try {
            if (StringUtils.isBlank(userCode)) {
                throw WalletException.badRequest(WalletConstants.INVALID_USER_CODE);
            }
            
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
                    
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                throw WalletException.badRequest(
                    String.format(WalletConstants.WALLET_INACTIVE, userCode));
            }
            
            return mapToWalletResponse(wallet);
            
        } catch (WalletException ex) {
            log.error("Error retrieving wallet: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error retrieving wallet: {}", ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public WalletTransactionResponse depositFunds(String userCode, WalletTransactionRequest request) {
        try {
            validateTransactionRequest(userCode, request);
            
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
                    
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                throw WalletException.badRequest(
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
            
        } catch (WalletException ex) {
            log.error(WalletConstants.TRANSACTION_FAILED + " for user {}: {}", userCode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("{} for user {}: {}", WalletConstants.INTERNAL_SERVER_ERROR, userCode, ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public WalletTransactionResponse withdrawFunds(String userCode, WalletTransactionRequest request) {
        try {
            validateTransactionRequest(userCode, request);
            
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
            
            if (wallet.getWalletStatus() != HoldStatus.ACTIVE) {
                throw WalletException.badRequest(
                    String.format(WalletConstants.WALLET_INACTIVE, userCode));
            }
            
            double amount = request.getAmount().doubleValue();
            if (wallet.getAvailableBalance() < amount) {
                throw WalletException.badRequest(WalletConstants.INSUFFICIENT_FUNDS);
            }

            double newBalance = wallet.getBalance() - amount;
            double newAvailableBalance = wallet.getAvailableBalance() - amount;

            wallet.setBalance(newBalance);
            wallet.setAvailableBalance(newAvailableBalance);
            wallet.setUpdatedAt(LocalDateTime.now());
            wallet.setLastModifiedBy(userCode);
            
            Wallet updatedWallet = walletRepository.save(wallet);
            
            return createTransactionResponse(updatedWallet, amount, "WITHDRAWAL");
            
        } catch (WalletException ex) {
            log.error("Error processing withdrawal for user {}: {}", userCode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing withdrawal for user {}: {}", userCode, ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    public Double getWalletBalance(String userCode) {
        try {
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));

            return wallet.getBalance();

        } catch (WalletException ex) {
            log.error("Error getting balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error getting balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Double getAvailableBalance(String userCode) {
        try {
            Wallet wallet = walletRepository.findByUserCode(userCode)
                .orElseThrow(() -> WalletException.notFound(
                    String.format(WalletConstants.WALLET_NOT_FOUND, userCode)));
                    
            return wallet.getAvailableBalance();
            
        } catch (WalletException ex) {
            log.error("Error getting available balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error getting available balance for user {}: {}", userCode, ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public boolean walletExists(String userCode) {
        try {
            return walletRepository.existsByUserCode(userCode);
        } catch (Exception ex) {
            log.error("Error checking if wallet exists for user {}: {}", userCode, ex.getMessage(), ex);
            throw WalletException.internalServerError(WalletConstants.INTERNAL_SERVER_ERROR);
        }
    }


    // Helper Methods
    
    private void validateWalletRequest(WalletCreateRequest request) {
        if (request == null) {
            throw WalletException.badRequest(WalletConstants.INVALID_REQUEST);
        }
        
        if (StringUtils.isBlank(request.getUserCode())) {
            throw WalletException.badRequest(WalletConstants.INVALID_USER_CODE);
        }
        
        if (request.getCurrency() == null) {
            throw WalletException.badRequest(WalletConstants.INVALID_CURRENCY);
        }
    }
    
    private void validateTransactionRequest(String userCode, WalletTransactionRequest request) {
        if (StringUtils.isBlank(userCode)) {
            throw WalletException.badRequest(WalletConstants.INVALID_USER_CODE);
        }
        
        if (request == null) {
            throw WalletException.badRequest(WalletConstants.INVALID_REQUEST);
        }
        
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw WalletException.badRequest(WalletConstants.INVALID_AMOUNT);
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
