package com.nkeanyi.payment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_db_id", nullable = false)
    private Long paymentDbId;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Column(name = "entry_type", nullable = false, length = 40)
    private String entryType;

    @Column(name = "account_ref", nullable = false, length = 64)
    private String accountRef;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "direction", nullable = false, length = 20)
    private String direction;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(Long paymentDbId, String paymentId, String entryType,
                       String accountRef, BigDecimal amount, String currency,
                       String direction, String status) {
        this.paymentDbId = paymentDbId;
        this.paymentId = paymentId;
        this.entryType = entryType;
        this.accountRef = accountRef;
        this.amount = amount;
        this.currency = currency;
        this.direction = direction;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
