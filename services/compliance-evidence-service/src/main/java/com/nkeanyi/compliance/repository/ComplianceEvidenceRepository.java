package com.nkeanyi.compliance.repository;

import com.nkeanyi.compliance.entity.ComplianceEvidence;
import com.nkeanyi.compliance.entity.EvidenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceEvidenceRepository extends JpaRepository<ComplianceEvidence, UUID> {

    List<ComplianceEvidence> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<ComplianceEvidence> findByTenantIdAndCorrelationIdOrderByCreatedAtDesc(String tenantId, String correlationId);

    List<ComplianceEvidence> findByDocumentIdOrderByCreatedAtDesc(String documentId);

    List<ComplianceEvidence> findByTenantIdAndPaymentIdOrderByCreatedAtDesc(String tenantId, String paymentId);

    List<ComplianceEvidence> findByEvidenceTypeOrderByCreatedAtDesc(EvidenceType evidenceType);
}
