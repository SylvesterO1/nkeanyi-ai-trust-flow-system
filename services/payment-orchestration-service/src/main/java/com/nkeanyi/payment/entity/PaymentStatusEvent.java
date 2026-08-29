package com.nkeanyi.payment.entity;

import com.nkeanyi.payment.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_status_events")
public class PaymentStatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_db_id", nullable = false)
    private Long paymentDbId;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private PaymentStatus toStatus;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "actor", length = 100)
    private String actor;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    public PaymentStatusEvent() {
    }

    public PaymentStatusEvent(Long paymentDbId, String paymentId, PaymentStatus fromStatus,
                              PaymentStatus toStatus, String reason, String actor) {
        this.paymentDbId = paymentDbId;
        this.paymentId = paymentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.actor = actor;
        this.eventTime = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.eventTime == null) {
            this.eventTime = LocalDateTime.now();
        }
    }
}
