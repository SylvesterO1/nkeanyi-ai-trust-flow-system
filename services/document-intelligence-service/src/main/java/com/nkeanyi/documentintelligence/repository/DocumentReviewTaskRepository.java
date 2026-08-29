package com.nkeanyi.documentintelligence.repository;

import com.nkeanyi.documentintelligence.entity.DocumentReviewTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentReviewTaskRepository extends JpaRepository<DocumentReviewTask, UUID> {
    List<DocumentReviewTask> findByDocumentIdOrderByCreatedAtDesc(String documentId);

    List<DocumentReviewTask> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<DocumentReviewTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);
}
