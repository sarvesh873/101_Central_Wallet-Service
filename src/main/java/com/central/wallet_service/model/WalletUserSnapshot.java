package com.central.wallet_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the minimal, denormalized snapshot of User data required by the Wallet Service.
 * This entity is considered 'Read-Only' from the perspective of the Wallet's business logic
 * and should ONLY be updated by the Kafka consumer processing UserUpdated events.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "wallet_user_snapshot")
public class WalletUserSnapshot {

    // The userCode is the Primary Key, matching the ID from the User Service.
    @Id
    @Column(name = "user_code", unique = true, nullable = false)
    private String userCode;

    // Duplicated and synchronized data.
    @Column(name = "username")
    private String username;

    @Column(nullable = false, length = 50)
    @Email
    private String email;

    @Column(nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-Many relationship with Wallet
    @OneToMany(mappedBy = "userSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Wallet> wallets = new HashSet<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
