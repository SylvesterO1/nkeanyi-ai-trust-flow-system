package com.nkeanyi.payment.repository;

import com.nkeanyi.payment.entity.PaymentRiskDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRiskDecisionRepository extends JpaRepository<PaymentRiskDecision, Long> {

    List<PaymentRiskDecision> findByPaymentIdOrderByCreatedAtDesc(String paymentId);
}
