package com.nkeanyi.compliance.controller;

import com.nkeanyi.compliance.dto.CreateEvidenceRequest;
import com.nkeanyi.compliance.dto.DemoDashboardResponse;
import com.nkeanyi.compliance.dto.EvidenceResponse;
import com.nkeanyi.compliance.dto.EvidenceSearchResponse;
import com.nkeanyi.compliance.dto.EvidenceSummaryResponse;
import com.nkeanyi.compliance.dto.TenantEvidenceSummaryResponse;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.service.ComplianceEvidenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
public class ComplianceEvidenceController {

    private final ComplianceEvidenceService service;

    public ComplianceEvidenceController(ComplianceEvidenceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceResponse createEvidence(@Valid @RequestBody CreateEvidenceRequest request) {
        return service.createEvidence(request);
    }

    @GetMapping("/{id}")
    public EvidenceResponse getById(@PathVariable("id") UUID id) {
        return service.getById(id);
    }

    @GetMapping("/tenant/{tenantId}")
    public EvidenceSearchResponse getByTenantId(@PathVariable("tenantId") String tenantId) {
        var records = service.getByTenantId(tenantId);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/tenant/{tenantId}/summary")
    public TenantEvidenceSummaryResponse getTenantSummary(@PathVariable("tenantId") String tenantId) {
        return service.getTenantSummary(tenantId);
    }

    @GetMapping("/correlation/{correlationId}")
    public EvidenceSearchResponse getByCorrelationId(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable("correlationId") String correlationId
    ) {
        var records = service.getByCorrelationId(tenantId, correlationId);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/correlation/{correlationId}/summary")
    public EvidenceSummaryResponse getCorrelationSummary(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable("correlationId") String correlationId
    ) {
        return service.getCorrelationSummary(tenantId, correlationId);
    }

    @GetMapping("/correlation/{correlationId}/certificate")
    public Object getCorrelationCertificate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable("correlationId") String correlationId
    ) {
        return service.getCorrelationCertificate(tenantId, correlationId);
    }

    @GetMapping("/document/{documentId}")
    public EvidenceSearchResponse getByDocumentId(@PathVariable("documentId") String documentId) {
        var records = service.getByDocumentId(documentId);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/payment/{paymentId}")
    public EvidenceSearchResponse getByPaymentId(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable("paymentId") String paymentId
    ) {
        var records = service.getByPaymentId(tenantId, paymentId);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/type/{evidenceType}")
    public EvidenceSearchResponse getByEvidenceType(@PathVariable("evidenceType") EvidenceType evidenceType) {
        var records = service.getByEvidenceType(evidenceType);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/demo-dashboard")
    public DemoDashboardResponse getDemoDashboard(
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader
    ) throws MissingRequestHeaderException {
        String resolvedTenantId = tenantId != null && !tenantId.isBlank() ? tenantId : tenantIdHeader;
        if (resolvedTenantId == null || resolvedTenantId.isBlank()) {
            throw new MissingRequestHeaderException("X-Tenant-Id", null);
        }

        return service.getDemoDashboard(resolvedTenantId);
    }
}
