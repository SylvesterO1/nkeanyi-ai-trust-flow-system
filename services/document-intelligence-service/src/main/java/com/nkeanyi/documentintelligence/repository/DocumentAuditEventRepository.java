package com.nkeanyi.documentintelligence.repository;

import com.nkeanyi.documentintelligence.entity.DocumentAuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentAuditEventRepository extends JpaRepository<DocumentAuditEvent, UUID> {
    List<DocumentAuditEvent> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);

    List<DocumentAuditEvent> findAllByOrderByTimestampDesc(Pageable pageable);
}
