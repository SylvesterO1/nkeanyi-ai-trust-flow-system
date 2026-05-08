package com.nkeanyi.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantCorrelationGatewayFilterTest {

    @Test
    void shouldExposeExpectedHeaderNames() {
        assertThat(TenantCorrelationGatewayFilter.TENANT_ID_HEADER)
                .isEqualTo("X-Tenant-Id");

        assertThat(TenantCorrelationGatewayFilter.CORRELATION_ID_HEADER)
                .isEqualTo("X-Correlation-Id");

        assertThat(TenantCorrelationGatewayFilter.REQUEST_SOURCE_HEADER)
                .isEqualTo("X-Request-Source");
    }
}
