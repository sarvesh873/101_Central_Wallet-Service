package com.central.wallet_service.scehduler;

import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletHold;
import com.central.wallet_service.model.WalletUserSnapshot;
import com.central.wallet_service.repository.WalletHoldRepository;
import com.central.wallet_service.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class WalletHoldExpiryScheduler {

    @Autowired
    private WalletHoldRepository holdRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public int processExpiredHolds() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("Processing expired holds at: {}", now);

            // Find all expired pending holds
            List<WalletHold> expiredHolds = holdRepository.findExpiredHolds(now, HoldStatus.ACTIVE);

            int processedCount = 0;

            for (WalletHold hold : expiredHolds) {
                try {
                    // Release the expired hold
                    hold.setStatus(HoldStatus.EXPIRED);
                    hold.setUpdatedAt(now);
                    hold.setDescription("Hold expired");

                    // Return funds to available balance
                    Wallet wallet = hold.getWallet();
                    wallet.setAvailableBalance(wallet.getAvailableBalance() + hold.getCapturedAmount());

                    holdRepository.save(hold);
                    walletRepository.save(wallet);


                    log.info("Released expired hold {} for wallet {} of user {}", hold.getId(), wallet.getId(), wallet.getUserSnapshot().getUserCode());
                    processedCount++;

                } catch (Exception ex) {
                    log.error("Error processing expired hold {}: {}", hold.getId(), ex.getMessage(), ex);
                }
            }

            log.info("Processed {} expired holds", processedCount);
            return processedCount;

        } catch (Exception ex) {
            log.error("Error in processExpiredHolds job: {}", ex.getMessage(), ex);
            return 0;
        }
    }
}
