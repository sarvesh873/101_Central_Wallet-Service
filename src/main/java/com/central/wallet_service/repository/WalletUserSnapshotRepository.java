package com.central.wallet_service.repository;

import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletUserSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WalletUserSnapshotRepository extends JpaRepository<WalletUserSnapshot, Long> {

}
