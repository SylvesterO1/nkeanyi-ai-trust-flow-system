package com.nkeanyi.payment.repository;

import com.nkeanyi.payment.entity.PaymentIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentIdempotencyKeyRepository extends JpaRepository<PaymentIdempotencyKey, Long> {

    Optional<PaymentIdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
