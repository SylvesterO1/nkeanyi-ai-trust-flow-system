package com.nkeanyi.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.UUID;

@Configuration
public class CorrelationIdFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Bean
    public GlobalFilter correlationIdGlobalFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(CORRELATION_ID_HEADER);

            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .build();

            return chain.filter(
                    exchange.mutate()
                            .request(mutatedRequest)
                            .build()
            );
        };
    }

    @Bean
    public Ordered gatewayFilterOrder() {
        return () -> -1;
    }
}
