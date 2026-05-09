package com.nkeanyi.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class GatewaySecurityConfigTest {

    private final GatewaySecurityConfig securityConfig = new GatewaySecurityConfig();

    @Test
    void shouldExtractScopesFromScopeClaim() {
        Jwt jwt = jwt(Map.of("scope", "payments.read payments.write"));

        Collection<GrantedAuthority> authorities = securityConfig.extractAuthorities(jwt);

        assertThat(authorityNames(authorities))
                .contains("SCOPE_payments.read", "SCOPE_payments.write");
    }

    @Test
    void shouldExtractScopesFromScpClaim() {
        Jwt jwt = jwt(Map.of("scp", List.of("evidence.read", "documents.write")));

        Collection<GrantedAuthority> authorities = securityConfig.extractAuthorities(jwt);

        assertThat(authorityNames(authorities))
                .contains("SCOPE_evidence.read", "SCOPE_documents.write");
    }

    @Test
    void shouldExtractScopesFromStringScpClaim() {
        Jwt jwt = jwt(Map.of("scp", "knowledge.read knowledge.write"));

        Collection<GrantedAuthority> authorities = securityConfig.extractAuthorities(jwt);

        assertThat(authorityNames(authorities))
                .contains("SCOPE_knowledge.read", "SCOPE_knowledge.write");
    }

    @Test
    void shouldExtractRealmAndResourceRolesFromKeycloakJwt() {
        Jwt jwt = jwt(Map.of(
                "realm_access", Map.of("roles", List.of("platform-admin")),
                "resource_access", Map.of(
                        "payment-cli", Map.of("roles", List.of("payment-operator")),
                        "api-gateway", Map.of("roles", List.of("gateway-user"))
                )
        ));

        Collection<GrantedAuthority> authorities = securityConfig.extractAuthorities(jwt);

        assertThat(authorityNames(authorities))
                .contains("ROLE_platform-admin", "ROLE_payment-operator", "ROLE_gateway-user");
    }

    private List<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                claims
        );
    }
}
