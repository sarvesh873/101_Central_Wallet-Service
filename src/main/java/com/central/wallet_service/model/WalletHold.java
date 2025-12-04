package com.central.wallet_service.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "wallet_holds")
public class WalletHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(nullable = false, unique = true)
    private String holdId;  // External unique ID for each hold

    @Column(nullable = false, unique = true)
    private String transaction_id;  // External reference ID (e.g., order ID)

    @Column(nullable = false)
    private Double originalAmount;

    @Column(nullable = false)
    private Double remainingAmount;

    @Column(nullable = false)
    private Double capturedAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private HoldStatus status ;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata;

    @Column(name = "capture_transaction_id")
    private String captureTransactionId;

    @Column(name = "release_transaction_id")
    private String releaseTransactionId;

    // Helper method to check if hold is active
    public boolean isActive() {
        return status == HoldStatus.ACTIVE && expiresAt.isAfter(LocalDateTime.now());
    }

    @PrePersist
    public void prePersist() {
        if (this.remainingAmount == null) {
            this.remainingAmount = this.originalAmount;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(10);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
