package com.central.wallet_service.repository;

import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletHold;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalletHoldRepository extends JpaRepository<WalletHold, Long> {
    
    /**
     * Find a hold by its unique hold ID
     * @param holdId The hold ID to search for
     * @return Optional containing the hold if found
     */
    Optional<WalletHold> findByHoldId(String holdId);
    
    /**
     * Find all holds for a specific wallet
     * @param wallet The wallet to find holds for
     * @param pageable Pagination information
     * @return Page of holds
     */
    Page<WalletHold> findByWallet(Wallet wallet, Pageable pageable);
    
    /**
     * Find expired holds that are still in PENDING status
     * @param currentTime The current time to check against
     * @param status The status to filter by (typically PENDING)
     * @return List of expired holds
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM WalletHold h WHERE h.expiresAt <= :currentTime AND h.status = :status")
    List<WalletHold> findExpiredHolds(
        @Param("currentTime") LocalDateTime currentTime,
        @Param("status") HoldStatus status
    );
    
    /**
     * Find all holds matching the given specification
     * @param spec The specification to filter by
     * @param pageable Pagination information
     * @return Page of holds matching the specification
     */
    Page<WalletHold> findAll(Specification<WalletHold> spec, Pageable pageable);
    
    /**
     * Calculate the total amount of PENDING holds for a wallet
     * @param walletId The wallet ID to calculate for
     * @return The total amount of PENDING holds
     */
    @Query("SELECT COALESCE(SUM(h.capturedAmount), 0) FROM WalletHold h WHERE h.wallet.id = :walletId AND h.status = 'ACTIVE'")
    double sumPendingHoldsByWalletId(@Param("walletId") Long walletId);
    
    /**
     * Count the number of holds matching the given specification
     * @param spec The specification to filter by
     * @return The count of matching holds
     */
    long count(Specification<WalletHold> spec);
}
