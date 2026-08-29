package com.nkeanyi.payment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_risk_decisions")
public class PaymentRiskDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_db_id", nullable = false)
    private Long paymentDbId;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Column(name = "decision", nullable = false, length = 40)
    private String decision;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "rules_triggered", columnDefinition = "text")
    private String rulesTriggered;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PaymentRiskDecision() {
    }

    public PaymentRiskDecision(Long paymentDbId, String paymentId, Double riskScore,
                               String decision, String reason, String rulesTriggered) {
        this.paymentDbId = paymentDbId;
        this.paymentId = paymentId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.reason = reason;
        this.rulesTriggered = rulesTriggered;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
