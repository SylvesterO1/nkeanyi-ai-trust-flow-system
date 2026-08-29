package com.nkeanyi.payment.repository;

import com.nkeanyi.payment.entity.PaymentStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentStatusEventRepository extends JpaRepository<PaymentStatusEvent, Long> {

    List<PaymentStatusEvent> findByPaymentIdOrderByEventTimeAsc(String paymentId);
}
