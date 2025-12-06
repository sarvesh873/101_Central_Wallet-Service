package com.central.wallet_service.specifications;

import com.central.wallet_service.model.Wallet;
import com.central.wallet_service.model.WalletHold;
import com.central.wallet_service.model.HoldStatus;
import com.central.wallet_service.model.WalletUserSnapshot;
import jakarta.persistence.criteria.Join;
import org.hibernate.query.common.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

public class WalletHoldSpecifications {

    private WalletHoldSpecifications() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a specification for filtering holds by user code
     * @param userCode The user code to filter by
     * @return Specification for the query
     */
    public static Specification<WalletHold> hasUserCode(String userCode) {
        return (root, query, criteriaBuilder) -> {
            if (StringUtils.isBlank(userCode)) {
                return criteriaBuilder.conjunction();
            }
            // Join WalletHold with Wallet
            Join<WalletHold, Wallet> walletJoin = root.join("wallet");

            // Join Wallet with WalletUserSnapshot
            Join<Wallet, WalletUserSnapshot> userSnapshotJoin = walletJoin.join("userSnapshot");

            // Match on userCode
            return criteriaBuilder.equal(userSnapshotJoin.get("userCode"), userCode);
        };
    }

    /**
     * Creates a specification for filtering holds by status
     * @param status The status to filter by
     * @return Specification for the query
     */
    public static Specification<WalletHold> hasStatus(HoldStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Creates a specification for filtering holds by currency
     * @param currency The currency code to filter by (case-insensitive)
     * @return Specification for the query
     */
    public static Specification<WalletHold> hasCurrency(String currency) {
        return (root, query, criteriaBuilder) -> {
            if (StringUtils.isBlank(currency)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("wallet").get("currency")),
                currency.toUpperCase()
            );
        };
    }

    /**
     * Creates a specification for filtering holds created after the given date
     * @param date The date to filter by
     * @return Specification for the query
     */
    public static Specification<WalletHold> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get("createdAt"),
                date
            );
        };
    }

    /**
     * Creates a specification for filtering holds created before the given date
     * @param date The date to filter by
     * @return Specification for the query
     */
    public static Specification<WalletHold> createdBefore(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get("createdAt"),
                date
            );
        };
    }
}
