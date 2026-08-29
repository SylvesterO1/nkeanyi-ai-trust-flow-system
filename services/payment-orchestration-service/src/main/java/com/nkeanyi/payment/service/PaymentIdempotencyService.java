package com.nkeanyi.payment.service;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.entity.PaymentIdempotencyKey;
import com.nkeanyi.payment.exception.IdempotencyConflictException;
import com.nkeanyi.payment.repository.PaymentIdempotencyKeyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PaymentIdempotencyService {

    private final PaymentIdempotencyKeyRepository repository;

    public PaymentIdempotencyService(PaymentIdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    public Optional<PaymentIdempotencyKey> findAndValidateReplay(String idempotencyKey,
                                                                 CreatePaymentRequest request) {
        Optional<PaymentIdempotencyKey> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        String currentHash = hashRequest(request);
        PaymentIdempotencyKey record = existing.get();

        if (!record.getRequestHash().equals(currentHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used with a different payment request body."
            );
        }

        record.setLastReplayedAt(LocalDateTime.now());
        record.setReplayCount(record.getReplayCount() == null ? 1L : record.getReplayCount() + 1L);
        repository.save(record);

        return Optional.of(record);
    }

    public void saveNewRecord(String idempotencyKey, CreatePaymentRequest request, Payment payment) {
        PaymentIdempotencyKey record = new PaymentIdempotencyKey();
        record.setIdempotencyKey(idempotencyKey);
        record.setCustomerId(request.getCustomerId());
        record.setRequestHash(hashRequest(request));
        record.setPaymentDbId(payment.getId());
        record.setPaymentId(payment.getPaymentId());
        repository.save(record);
    }

    public String hashRequest(CreatePaymentRequest request) {
        String canonical = String.join("|",
                safe(request.getCustomerId()),
                safe(request.getSourceAccount()),
                safe(request.getDestinationAccount()),
                request.getAmount() == null ? "" : request.getAmount().stripTrailingZeros().toPlainString(),
                safe(request.getCurrency()).toUpperCase(),
                safe(request.getPaymentReference()),
                safe(request.getPaymentMethod()),
                safe(request.getNarration())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash payment request for idempotency.", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
