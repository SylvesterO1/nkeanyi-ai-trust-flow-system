package com.nkeanyi.documentintelligence.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationTenantLoggingFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String tenantId = readHeader(request, TENANT_HEADER, "unknown-tenant");
        String correlationId = readHeader(request, CORRELATION_HEADER, "unknown-correlation");
        String idempotencyKey = readHeader(request, IDEMPOTENCY_HEADER, "unknown-idempotency-key");

        MDC.put(RegulatedRequestContext.MDC_TENANT_ID, tenantId);
        MDC.put(RegulatedRequestContext.MDC_CORRELATION_ID, correlationId);
        MDC.put(RegulatedRequestContext.MDC_IDEMPOTENCY_KEY, idempotencyKey);

        response.setHeader(TENANT_HEADER, tenantId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader(IDEMPOTENCY_HEADER, idempotencyKey);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RegulatedRequestContext.MDC_TENANT_ID);
            MDC.remove(RegulatedRequestContext.MDC_CORRELATION_ID);
            MDC.remove(RegulatedRequestContext.MDC_IDEMPOTENCY_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    private String readHeader(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        return value == null || value.isBlank() ? fallback : value;
    }
}
