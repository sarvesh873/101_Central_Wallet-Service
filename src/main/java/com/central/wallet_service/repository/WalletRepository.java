package com.central.wallet_service.repository;

import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    /**
     * Find a wallet by user code
     * @param userCode The user code to search for
     * @return Optional containing the wallet if found
     */

    @Query("SELECT w FROM Wallet w WHERE w.userSnapshot.userCode = :userCode")
    Optional<Wallet> findByUserCode(@Param("userCode") String userCode);



    /**
     * Check if a wallet exists for the given user code
     * @param userCode The user code to check
     * @return true if a wallet exists, false otherwise
     */
    @Query("SELECT COUNT(w) > 0 FROM Wallet w WHERE w.userSnapshot.userCode = :userCode")
    boolean existsByUserCode(@Param("userCode") String userCode);
    

}
