package com.senac.corpfinancialapi;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
class ApiCredential {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(nullable = false, unique = true, length = 64)
    private String secretHash;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private ApiKeyType type;

    protected ApiCredential() {
    }

    ApiCredential(UUID id, String label, String secretHash, Instant createdAt, Account account, ApiKeyType type) {
        this.id = id;
        this.label = label;
        this.secretHash = secretHash;
        this.createdAt = createdAt;
        this.account = account;
        this.type = type;
    }

    UUID id() {
        return id;
    }

    String label() {
        return label;
    }

    Account account() {
        return account;
    }

    ApiKeyType type() {
        return type;
    }

    Instant createdAt() {
        return createdAt;
    }
}
