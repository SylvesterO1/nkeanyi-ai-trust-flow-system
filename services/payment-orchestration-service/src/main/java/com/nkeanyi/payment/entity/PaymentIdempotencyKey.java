package com.nkeanyi.payment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_idempotency_keys",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_payment_idempotency_keys_key", columnNames = "idempotency_key")
       })
public class PaymentIdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "payment_db_id", nullable = false)
    private Long paymentDbId;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_replayed_at")
    private LocalDateTime lastReplayedAt;

    @Column(name = "replay_count", nullable = false)
    private Long replayCount = 0L;

    public PaymentIdempotencyKey() {
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.replayCount == null) {
            this.replayCount = 0L;
        }
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public Long getPaymentDbId() {
        return paymentDbId;
    }

    public void setPaymentDbId(Long paymentDbId) {
        this.paymentDbId = paymentDbId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastReplayedAt() {
        return lastReplayedAt;
    }

    public void setLastReplayedAt(LocalDateTime lastReplayedAt) {
        this.lastReplayedAt = lastReplayedAt;
    }

    public Long getReplayCount() {
        return replayCount;
    }

    public void setReplayCount(Long replayCount) {
        this.replayCount = replayCount;
    }

    public void markReplayed() {
        this.lastReplayedAt = LocalDateTime.now();
        this.replayCount = this.replayCount == null ? 1L : this.replayCount + 1L;
    }
}
