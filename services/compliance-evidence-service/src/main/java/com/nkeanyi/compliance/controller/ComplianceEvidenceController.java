package com.nkeanyi.compliance.controller;

import com.nkeanyi.compliance.dto.CreateEvidenceRequest;
import com.nkeanyi.compliance.dto.EvidenceResponse;
import com.nkeanyi.compliance.dto.EvidenceSearchResponse;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.service.ComplianceEvidenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/correlation/{correlationId}")
    public EvidenceSearchResponse getByCorrelationId(@PathVariable("correlationId") String correlationId) {
        var records = service.getByCorrelationId(correlationId);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/document/{documentId}")
    public EvidenceSearchResponse getByDocumentId(@PathVariable("documentId") String documentId) {
        var records = service.getByDocumentId(documentId);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/payment/{paymentId}")
    public EvidenceSearchResponse getByPaymentId(@PathVariable("paymentId") String paymentId) {
        var records = service.getByPaymentId(paymentId);
        return new EvidenceSearchResponse(records.size(), records);
    }

    @GetMapping("/type/{evidenceType}")
    public EvidenceSearchResponse getByEvidenceType(@PathVariable("evidenceType") EvidenceType evidenceType) {
        var records = service.getByEvidenceType(evidenceType);
        return new EvidenceSearchResponse(records.size(), records);
    }
}
