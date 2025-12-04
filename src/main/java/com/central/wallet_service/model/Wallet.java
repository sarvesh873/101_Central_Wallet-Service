package com.central.wallet_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id", nullable = false)
    private Long id;

    @Column(name = "balance", nullable = false)
    private Double balance;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "available_balance", nullable = false)
    private Double availableBalance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "transaction_id")
    private String transaction_id;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "wallet_status")
    @Enumerated(EnumType.STRING)
    private HoldStatus walletStatus;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;


    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<WalletHold> holds = new HashSet<>();

    // Foreign Key Reference to the WalletUserSnapshot
    // This establishes the 'connection' without duplicating the user's name/email directly here.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_code", referencedColumnName = "user_code", nullable = false)
    private WalletUserSnapshot userSnapshot;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.walletStatus = HoldStatus.ACTIVE;
        this.availableBalance = 0.0;
        this.balance = 0.0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}

