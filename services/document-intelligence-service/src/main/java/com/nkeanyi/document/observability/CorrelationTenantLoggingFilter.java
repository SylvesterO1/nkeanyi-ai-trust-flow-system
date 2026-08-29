package com.nkeanyi.document.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class CorrelationTenantLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationTenantLoggingFilter.class);

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_IDEMPOTENCY_KEY = "idempotencyKey";

    private static final String UNKNOWN_TENANT = "unknown-tenant";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String tenantId = Optional.ofNullable(request.getHeader(TENANT_HEADER))
                .filter(value -> !value.isBlank())
                .orElse(UNKNOWN_TENANT);

        String correlationId = Optional.ofNullable(request.getHeader(CORRELATION_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> "generated-" + UUID.randomUUID());
        String idempotencyKey = Optional.ofNullable(request.getHeader(IDEMPOTENCY_HEADER))
                .filter(value -> !value.isBlank())
                .orElse("unknown-idempotency-key");

        MDC.put(MDC_TENANT_ID, tenantId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_IDEMPOTENCY_KEY, idempotencyKey);

        response.setHeader(TENANT_HEADER, tenantId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader(IDEMPOTENCY_HEADER, idempotencyKey);

        try {
            log.info(
                    "request_started method={} uri={} tenantId={} correlationId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    tenantId,
                    correlationId
            );

            filterChain.doFilter(request, response);

        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            log.info(
                    "request_completed method={} uri={} status={} durationMs={} tenantId={} correlationId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    tenantId,
                    correlationId
            );

            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_IDEMPOTENCY_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/health");
    }
}
