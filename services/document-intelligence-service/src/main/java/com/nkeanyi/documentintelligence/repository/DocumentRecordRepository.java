package com.nkeanyi.documentintelligence.repository;

import com.nkeanyi.documentintelligence.entity.DocumentRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, UUID> {
    Optional<DocumentRecord> findByDocumentId(String documentId);

    List<DocumentRecord> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<DocumentRecord> findByCorrelationIdOrderByCreatedAtDesc(String correlationId, Pageable pageable);

    List<DocumentRecord> findByTenantIdAndCorrelationIdOrderByCreatedAtDesc(
            String tenantId,
            String correlationId,
            Pageable pageable
    );

    List<DocumentRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
