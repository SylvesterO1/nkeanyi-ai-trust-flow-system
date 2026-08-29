package com.nkeanyi.documentintelligence.repository;

import com.nkeanyi.documentintelligence.entity.DocumentFeedbackEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentFeedbackEventRepository extends JpaRepository<DocumentFeedbackEvent, UUID> {
}
