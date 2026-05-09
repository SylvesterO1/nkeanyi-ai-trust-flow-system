package com.nkeanyi.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class TenantCorrelationGatewayFilterTest {

    private TenantCorrelationGatewayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantCorrelationGatewayFilter();
    }

    @Test
    void shouldExposeExpectedHeaderNames() {
        assertThat(TenantCorrelationGatewayFilter.TENANT_ID_HEADER)
                .isEqualTo("X-Tenant-Id");

        assertThat(TenantCorrelationGatewayFilter.CORRELATION_ID_HEADER)
                .isEqualTo("X-Correlation-Id");

        assertThat(TenantCorrelationGatewayFilter.REQUEST_SOURCE_HEADER)
                .isEqualTo("X-Request-Source");
    }

    @Test
    void shouldAllowActuatorHealthWithoutTenantHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build()
        );
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwardedExchange)).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(forwardedExchange.get()).isNotNull();
    }

    @Test
    void shouldRejectProtectedRequestWithoutTenantHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/payments").build()
        );

        filter.filter(exchange, unusedChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"status\": 400")
                .contains("Missing required header: X-Tenant-Id");
    }

    @Test
    void shouldRejectInvalidTenantHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/payments")
                        .header(TenantCorrelationGatewayFilter.TENANT_ID_HEADER, "bad tenant!")
                        .build()
        );

        filter.filter(exchange, unusedChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("Invalid tenant header format: X-Tenant-Id");
    }

    @Test
    void shouldPropagateTenantCorrelationAndGatewaySourceHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/payments")
                        .header(TenantCorrelationGatewayFilter.TENANT_ID_HEADER, "tenant-001")
                        .header(TenantCorrelationGatewayFilter.CORRELATION_ID_HEADER, "corr-001")
                        .header(TenantCorrelationGatewayFilter.REQUEST_SOURCE_HEADER, "untrusted-client")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwardedExchange)).block();

        assertThat(forwardedExchange.get().getRequest().getHeaders()
                .getFirst(TenantCorrelationGatewayFilter.TENANT_ID_HEADER))
                .isEqualTo("tenant-001");
        assertThat(forwardedExchange.get().getRequest().getHeaders()
                .getFirst(TenantCorrelationGatewayFilter.CORRELATION_ID_HEADER))
                .isEqualTo("corr-001");
        assertThat(forwardedExchange.get().getRequest().getHeaders()
                .getFirst(TenantCorrelationGatewayFilter.REQUEST_SOURCE_HEADER))
                .isEqualTo(TenantCorrelationGatewayFilter.REQUEST_SOURCE);
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/payments")
                        .header(TenantCorrelationGatewayFilter.TENANT_ID_HEADER, "tenant-001")
                        .build()
        );
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, capturingChain(forwardedExchange)).block();

        assertThat(forwardedExchange.get().getRequest().getHeaders()
                .getFirst(TenantCorrelationGatewayFilter.CORRELATION_ID_HEADER))
                .startsWith("corr-");
    }

    private GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> forwardedExchange) {
        return exchange -> {
            forwardedExchange.set(exchange);
            return reactor.core.publisher.Mono.empty();
        };
    }

    private GatewayFilterChain unusedChain() {
        return exchange -> {
            throw new AssertionError("Request should have been rejected before reaching the chain");
        };
    }
}
