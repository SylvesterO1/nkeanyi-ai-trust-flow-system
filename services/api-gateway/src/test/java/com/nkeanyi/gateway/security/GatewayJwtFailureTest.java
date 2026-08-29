package com.nkeanyi.gateway.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nkeanyi.gateway.filter.TenantCorrelationGatewayFilter;

import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@TestExecutionListeners(
        listeners = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
class GatewayJwtFailureTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRejectMalformedJwt() {
        getEvidenceWithToken("malformed")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectExpiredJwt() {
        getEvidenceWithToken("expired")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectValidJwtMissingRequiredScope() {
        getEvidenceWithToken("payments-only")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Authenticated token does not have the required scope");
    }

    @Test
    void shouldRejectJwtFromWrongIssuerWhenDecoderValidatesIssuer() {
        getEvidenceWithToken("wrong-issuer")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectJwtWithWrongAudienceWhenDecoderValidatesAudience() {
        getEvidenceWithToken("wrong-audience")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private WebTestClient.RequestHeadersSpec<?> getEvidenceWithToken(String token) {
        return webTestClient.get()
                .uri("/api/v1/evidence/correlation/corr-jwt")
                .header(TenantCorrelationGatewayFilter.TENANT_ID_HEADER, "tenant-jwt")
                .header(TenantCorrelationGatewayFilter.CORRELATION_ID_HEADER, "corr-jwt")
                .header(TenantCorrelationGatewayFilter.IDEMPOTENCY_KEY_HEADER, "idem-jwt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @TestConfiguration
    static class JwtDecoderConfig {

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> switch (token) {
                case "malformed" -> Mono.error(new BadJwtException("malformed JWT"));
                case "expired" -> Mono.error(validationFailure("invalid_token", "Jwt expired at 2026-05-23T00:00:00Z"));
                case "wrong-issuer" -> Mono.error(validationFailure("invalid_token", "The iss claim is not valid"));
                case "wrong-audience" -> Mono.error(validationFailure("invalid_token", "The aud claim is not valid"));
                case "payments-only" -> Mono.just(jwt(token, Map.of("scope", "payments.read")));
                default -> Mono.just(jwt(token, Map.of("scope", "evidence.read")));
            };
        }

        private static Jwt jwt(String token, Map<String, Object> claims) {
            Instant now = Instant.now();
            return new Jwt(
                    token,
                    now,
                    now.plusSeconds(300),
                    Map.of("alg", "none"),
                    withStandardClaims(claims, now)
            );
        }

        private static Map<String, Object> withStandardClaims(Map<String, Object> claims, Instant now) {
            java.util.LinkedHashMap<String, Object> standardClaims = new java.util.LinkedHashMap<>();
            standardClaims.put(JwtClaimNames.ISS, "http://localhost:8081/realms/payment-platform");
            standardClaims.put(JwtClaimNames.SUB, "gateway-test-user");
            standardClaims.put(JwtClaimNames.AUD, List.of("api-gateway"));
            standardClaims.put(JwtClaimNames.IAT, now);
            standardClaims.putAll(claims);
            return standardClaims;
        }

        private static JwtValidationException validationFailure(String code, String description) {
            return new JwtValidationException(
                    description,
                    List.of(new OAuth2Error(code, description, null))
            );
        }
    }
}
