package com.nkeanyi.documentintelligence.controller;

import com.nkeanyi.documentintelligence.model.DocumentAnalyzeRequest;
import com.nkeanyi.documentintelligence.model.DocumentAnalyzeResponse;
import com.nkeanyi.documentintelligence.model.DocumentAuditEventsResponse;
import com.nkeanyi.documentintelligence.model.DocumentFeedbackRequest;
import com.nkeanyi.documentintelligence.model.DocumentFeedbackResponse;
import com.nkeanyi.documentintelligence.model.DocumentLayoutPreviewResponse;
import com.nkeanyi.documentintelligence.model.DocumentOperationsMetricsResponse;
import com.nkeanyi.documentintelligence.model.DocumentProcessResponse;
import com.nkeanyi.documentintelligence.model.DocumentRecordResponse;
import com.nkeanyi.documentintelligence.model.DocumentRecordsResponse;
import com.nkeanyi.documentintelligence.model.DocumentReviewRequest;
import com.nkeanyi.documentintelligence.model.DocumentReviewTasksResponse;
import com.nkeanyi.documentintelligence.model.DocumentWorkflowResponse;
import com.nkeanyi.documentintelligence.service.DocumentAnalysisService;
import com.nkeanyi.documentintelligence.service.DocumentLayoutPreviewService;
import com.nkeanyi.documentintelligence.service.DocumentProcessingService;
import com.nkeanyi.documentintelligence.service.EnterpriseDocumentWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentIntelligenceController {

    private final DocumentAnalysisService documentAnalysisService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentLayoutPreviewService documentLayoutPreviewService;
    private final EnterpriseDocumentWorkflowService enterpriseDocumentWorkflowService;

    public DocumentIntelligenceController(
            DocumentAnalysisService documentAnalysisService,
            DocumentProcessingService documentProcessingService,
            DocumentLayoutPreviewService documentLayoutPreviewService,
            EnterpriseDocumentWorkflowService enterpriseDocumentWorkflowService
    ) {
        this.documentAnalysisService = documentAnalysisService;
        this.documentProcessingService = documentProcessingService;
        this.documentLayoutPreviewService = documentLayoutPreviewService;
        this.enterpriseDocumentWorkflowService = enterpriseDocumentWorkflowService;
    }

    @PostMapping("/analyze")
    public DocumentAnalyzeResponse analyze(@Valid @RequestBody DocumentAnalyzeRequest request) {
        return documentAnalysisService.analyze(request.documentName(), request.documentType());
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentProcessResponse process(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return documentProcessingService.process(file, idempotencyKey);
    }

    @PostMapping(value = "/layout-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentLayoutPreviewResponse layoutPreview(@RequestPart("file") MultipartFile file) {
        return documentLayoutPreviewService.preview(file);
    }

    @GetMapping
    public DocumentRecordsResponse listDocuments(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "25") int limit,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        String effectiveTenantId = hasText(tenantId) ? tenantId : tenantHeader;
        List<DocumentRecordResponse> records = documentProcessingService.listRecords(
                effectiveTenantId,
                correlationId,
                limit
        );
        return new DocumentRecordsResponse(records, records.size());
    }

    @GetMapping("/correlation/{correlationId}")
    public DocumentRecordsResponse findDocumentsByCorrelationId(
            @PathVariable String correlationId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "25") int limit,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        String effectiveTenantId = hasText(tenantId) ? tenantId : tenantHeader;
        List<DocumentRecordResponse> records = documentProcessingService.findRecordsByCorrelationId(
                correlationId,
                effectiveTenantId,
                limit
        );
        return new DocumentRecordsResponse(records, records.size());
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentRecordResponse> getDocument(@PathVariable String documentId) {
        return documentProcessingService.findRecordByDocumentId(documentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/enterprise/{documentId}/workflow")
    public ResponseEntity<DocumentWorkflowResponse> getWorkflow(@PathVariable String documentId) {
        return enterpriseDocumentWorkflowService.findWorkflowResponse(documentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/enterprise/reviews")
    public DocumentReviewTasksResponse reviewTasks(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "25") int limit,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return enterpriseDocumentWorkflowService.listReviewTasks(
                hasText(tenantId) ? tenantId : tenantHeader,
                status,
                riskLevel,
                limit
        );
    }

    @PostMapping("/enterprise/{documentId}/reviews")
    public DocumentWorkflowResponse reviewDocument(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentReviewRequest request
    ) {
        return enterpriseDocumentWorkflowService.reviewDocument(documentId, request);
    }

    @PostMapping("/enterprise/{documentId}/publish")
    public DocumentWorkflowResponse publishDocument(
            @PathVariable String documentId,
            @RequestParam(required = false) String actorId
    ) {
        return enterpriseDocumentWorkflowService.publishDocument(documentId, actorId);
    }

    @PostMapping("/enterprise/{documentId}/feedback")
    public DocumentFeedbackResponse feedback(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentFeedbackRequest request
    ) {
        return enterpriseDocumentWorkflowService.recordFeedback(documentId, request);
    }

    @GetMapping("/enterprise/audit")
    public DocumentAuditEventsResponse audit(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String documentId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "25") int limit,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        return enterpriseDocumentWorkflowService.searchAuditEvents(
                hasText(tenantId) ? tenantId : tenantHeader,
                documentId,
                userId,
                action,
                from,
                to,
                limit
        );
    }

    @GetMapping("/enterprise/metrics")
    public DocumentOperationsMetricsResponse metrics() {
        return enterpriseDocumentWorkflowService.metrics();
    }

    @GetMapping("/ping")
    public String ping() {
        return "document-intelligence-service-ok";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
