package com.central.wallet_service.service;

import com.central.wallet_service.dto.WalletCreateRequestDto;
import com.central.wallet_service.dto.WalletResponseDto;
import com.central.wallet_service.dto.WalletTransactionRequestDto;
import com.central.wallet_service.dto.WalletTransactionResponseDto;

public interface WalletService {
    
    /**
     * Creates a new wallet for a user
     * @param walletCreateRequest Contains wallet creation details
     * @return Created wallet details
     */
    WalletResponseDto createWallet(WalletCreateRequestDto walletCreateRequest);
    
    /**
     * Retrieves wallet details by user code
     * @param userCode Unique identifier for the user
     * @return Wallet details
     */
    WalletResponseDto getWalletByUserCode(String userCode);
    
    /**
     * Deposits funds into a wallet
     * @param userCode Unique identifier for the user
     * @param request Transaction details
     * @return Transaction response
     */
    WalletTransactionResponseDto depositFunds(String userCode, WalletTransactionRequestDto request);
    
    /**
     * Withdraws funds from a wallet
     * @param userCode Unique identifier for the user
     * @param request Transaction details
     * @return Transaction response
     */
    WalletTransactionResponseDto withdrawFunds(String userCode, WalletTransactionRequestDto request);
    
    /**
     * Gets the available balance (balance minus holds) of a wallet
     * @param userCode Unique identifier for the user
     * @return Available balance
     */
    Double getAvailableBalance(String userCode);


    /**
     * Checks if a wallet exists for the given user code
     * @param userCode Unique identifier for the user
     * @return true if wallet exists, false otherwise
     */
    boolean walletExists(String userCode);
}
