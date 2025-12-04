package com.central.wallet_service.service;

import com.central.wallet_service.model.Wallet;
import org.openapitools.model.*;

public interface WalletService {
    
    /**
     * Creates a new wallet for a user
     * @param walletCreateRequest Contains wallet creation details
     * @return Created wallet details
     */
    WalletResponse createWallet(WalletCreateRequest walletCreateRequest);
    
    /**
     * Retrieves wallet details by user code
     * @param userCode Unique identifier for the user
     * @return Wallet details
     */
    WalletResponse getWalletByUserCode(String userCode);
    
    /**
     * Deposits funds into a wallet
     * @param userCode Unique identifier for the user
     * @param request Transaction details
     * @return Transaction response
     */
    WalletTransactionResponse depositFunds(String userCode, WalletTransactionRequest request);
    
    /**
     * Withdraws funds from a wallet
     * @param userCode Unique identifier for the user
     * @param request Transaction details
     * @return Transaction response
     */
    WalletTransactionResponse withdrawFunds(String userCode, WalletTransactionRequest request);
    
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
