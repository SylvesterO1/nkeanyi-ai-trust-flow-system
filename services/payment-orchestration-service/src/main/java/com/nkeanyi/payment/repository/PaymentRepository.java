package com.nkeanyi.payment.repository;

import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.math.BigDecimal;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    long countByCustomerIdAndCreatedAtAfter(String customerId, LocalDateTime cutoff);

    long countByDestinationAccountAndCreatedAtAfter(String destinationAccount, LocalDateTime cutoff);

    long countByCustomerIdAndAmountGreaterThanEqualAndCreatedAtAfter(String customerId,
                                                                     BigDecimal amount,
                                                                     LocalDateTime cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Payment p
           set p.status = :status,
               p.decisionReason = :reason,
               p.riskScore = :riskScore,
               p.updatedAt = :updatedAt,
               p.processedAt = :processedAt
         where p.id = :paymentId
    """)
    int updateTerminalState(@Param("paymentId") Long paymentId,
                            @Param("status") PaymentStatus status,
                            @Param("reason") String reason,
                            @Param("riskScore") Double riskScore,
                            @Param("updatedAt") LocalDateTime updatedAt,
                            @Param("processedAt") LocalDateTime processedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Payment p
           set p.status = :status,
               p.decisionReason = :reason,
               p.riskScore = :riskScore,
               p.updatedAt = :updatedAt
         where p.id = :paymentId
    """)
    int updateNonTerminalState(@Param("paymentId") Long paymentId,
                               @Param("status") PaymentStatus status,
                               @Param("reason") String reason,
                               @Param("riskScore") Double riskScore,
                               @Param("updatedAt") LocalDateTime updatedAt);
}
