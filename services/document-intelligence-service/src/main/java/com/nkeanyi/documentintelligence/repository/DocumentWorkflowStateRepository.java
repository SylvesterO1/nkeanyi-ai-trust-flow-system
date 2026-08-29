package com.nkeanyi.documentintelligence.repository;

import com.nkeanyi.documentintelligence.entity.DocumentWorkflowState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentWorkflowStateRepository extends JpaRepository<DocumentWorkflowState, UUID> {
    Optional<DocumentWorkflowState> findByDocumentId(String documentId);

    List<DocumentWorkflowState> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<DocumentWorkflowState> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByLifecycleState(String lifecycleState);
}
