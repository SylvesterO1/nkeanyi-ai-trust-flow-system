package com.nkeanyi.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class TenantCorrelationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantCorrelationGatewayFilter.class);

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_SOURCE_HEADER = "X-Request-Source";

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/actuator",
            "/health",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();

        String path = originalRequest.getURI().getPath();
        String method = originalRequest.getMethod() != null
                ? originalRequest.getMethod().name()
                : "UNKNOWN";

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        HttpHeaders headers = originalRequest.getHeaders();

        String tenantId = firstNonBlank(headers.getFirst(TENANT_ID_HEADER));
        String correlationId = firstNonBlank(headers.getFirst(CORRELATION_ID_HEADER));

        if (tenantId == null) {
            return reject(exchange, HttpStatus.BAD_REQUEST,
                    "Missing required header: " + TENANT_ID_HEADER);
        }

        if (!isValidTenantId(tenantId)) {
            return reject(exchange, HttpStatus.BAD_REQUEST,
                    "Invalid tenant header format: " + TENANT_ID_HEADER);
        }

        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID();
        }

        String requestSource = firstNonBlank(headers.getFirst(REQUEST_SOURCE_HEADER));
        if (requestSource == null) {
            requestSource = "api-gateway";
        }

        ServerHttpRequest mutatedRequest = originalRequest.mutate()
                .header(TENANT_ID_HEADER, tenantId)
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(REQUEST_SOURCE_HEADER, requestSource)
                .build();

        log.info(
                "gateway_request_allowed tenantId={} correlationId={} method={} path={} source={} timestamp={}",
                tenantId,
                correlationId,
                method,
                path,
                requestSource,
                Instant.now()
        );

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String firstNonBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isValidTenantId(String tenantId) {
        return tenantId.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]{2,63}$");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : "UNKNOWN";

        log.warn(
                "gateway_request_rejected status={} method={} path={} reason={} timestamp={}",
                status.value(),
                method,
                path,
                message,
                Instant.now()
        );

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String responseBody = """
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "timestamp": "%s"
                }
                """.formatted(
                status.value(),
                status.getReasonPhrase(),
                message,
                Instant.now()
        );

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(responseBody.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
