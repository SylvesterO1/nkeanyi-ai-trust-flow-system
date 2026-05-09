package com.nkeanyi.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class GatewaySecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityConfig.class);

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange

                        // Public operational endpoints
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/actuator/info").permitAll()

                        // Compliance evidence service policies
                        .pathMatchers(HttpMethod.GET, "/api/v1/evidence/**")
                        .hasAuthority("SCOPE_evidence.read")

                        .pathMatchers(HttpMethod.POST, "/api/v1/evidence/**")
                        .hasAuthority("SCOPE_evidence.write")

                        .pathMatchers(HttpMethod.PUT, "/api/v1/evidence/**")
                        .hasAuthority("SCOPE_evidence.write")

                        .pathMatchers(HttpMethod.PATCH, "/api/v1/evidence/**")
                        .hasAuthority("SCOPE_evidence.write")

                        .pathMatchers(HttpMethod.DELETE, "/api/v1/evidence/**")
                        .hasAuthority("SCOPE_evidence.write")

                        // Payment service policies
                        .pathMatchers(HttpMethod.GET, "/api/v1/payments/**")
                        .hasAuthority("SCOPE_payments.read")

                        .pathMatchers(HttpMethod.POST, "/api/v1/payments/**")
                        .hasAuthority("SCOPE_payments.write")

                        .pathMatchers(HttpMethod.PUT, "/api/v1/payments/**")
                        .hasAuthority("SCOPE_payments.write")

                        .pathMatchers(HttpMethod.PATCH, "/api/v1/payments/**")
                        .hasAuthority("SCOPE_payments.write")

                        .pathMatchers(HttpMethod.DELETE, "/api/v1/payments/**")
                        .hasAuthority("SCOPE_payments.write")

                        // Document intelligence service policies
                        .pathMatchers(HttpMethod.GET, "/api/v1/documents/**")
                        .hasAuthority("SCOPE_documents.read")

                        .pathMatchers(HttpMethod.POST, "/api/v1/documents/**")
                        .hasAuthority("SCOPE_documents.write")

                        // Knowledge service policies
                        .pathMatchers(HttpMethod.GET, "/api/v1/knowledge/**")
                        .hasAuthority("SCOPE_knowledge.read")

                        .pathMatchers(HttpMethod.POST, "/api/v1/knowledge/search")
                        .hasAuthority("SCOPE_knowledge.read")

                        .pathMatchers(HttpMethod.POST, "/api/v1/knowledge/search-packaged")
                        .hasAuthority("SCOPE_knowledge.read")

                        .pathMatchers(HttpMethod.POST, "/api/v1/knowledge/answer")
                        .hasAuthority("SCOPE_knowledge.read")

                        .pathMatchers(HttpMethod.POST, "/api/v1/knowledge/index-text")
                        .hasAuthority("SCOPE_knowledge.write")

                        .pathMatchers(HttpMethod.POST, "/api/v1/knowledge/documents")
                        .hasAuthority("SCOPE_knowledge.write")

                        .pathMatchers(HttpMethod.POST, "/api/v1/knowledge/**")
                        .hasAuthority("SCOPE_knowledge.write")

                        // Everything else must at least be authenticated
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                        .accessDeniedHandler(jsonAccessDeniedHandler())
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
            return Mono.just(new JwtAuthenticationToken(jwt, authorities));
        };
    }

    Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = extractScopeAuthorities(jwt);

        authorities.addAll(extractRealmRoleAuthorities(jwt));
        authorities.addAll(extractResourceRoleAuthorities(jwt));

        return authorities;
    }

    private List<GrantedAuthority> extractScopeAuthorities(Jwt jwt) {
        Object scopeClaim = jwt.getClaims().get("scope");

        if (scopeClaim instanceof String scopeString) {
            return List.of(scopeString.split(" "))
                    .stream()
                    .filter(scope -> !scope.isBlank())
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());
        }

        Object scpClaim = jwt.getClaims().get("scp");

        if (scpClaim instanceof String scpString) {
            return List.of(scpString.split(" "))
                    .stream()
                    .filter(scope -> !scope.isBlank())
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());
        }

        if (scpClaim instanceof Collection<?> scopes) {
            return scopes.stream()
                    .map(Object::toString)
                    .filter(scope -> !scope.isBlank())
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());
        }

        return new java.util.ArrayList<>();
    }

    private List<GrantedAuthority> extractRealmRoleAuthorities(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");

        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return new java.util.ArrayList<>();
        }

        Object roles = realmAccessMap.get("roles");

        if (!(roles instanceof Collection<?> roleCollection)) {
            return new java.util.ArrayList<>();
        }

        return roleCollection.stream()
                .map(Object::toString)
                .filter(role -> !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractResourceRoleAuthorities(Jwt jwt) {
        Object resourceAccess = jwt.getClaims().get("resource_access");

        if (!(resourceAccess instanceof Map<?, ?> resourceAccessMap)) {
            return new java.util.ArrayList<>();
        }

        return resourceAccessMap.values()
                .stream()
                .filter(Map.class::isInstance)
                .map(clientAccess -> (Map<String, Object>) clientAccess)
                .map(clientAccess -> clientAccess.get("roles"))
                .filter(Collection.class::isInstance)
                .map(roles -> (Collection<?>) roles)
                .flatMap(Collection::stream)
                .map(Object::toString)
                .filter(role -> !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    private ServerAuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (exchange, ex) -> writeJsonError(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Missing or invalid bearer token"
        );
    }

    private ServerAccessDeniedHandler jsonAccessDeniedHandler() {
        return (exchange, ex) -> writeJsonError(
                exchange,
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Authenticated token does not have the required scope"
        );
    }

    private Mono<Void> writeJsonError(
            ServerWebExchange exchange,
            HttpStatus status,
            String error,
            String message
    ) {
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
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", Instant.now().toString());

        String json = toJson(body);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String toJson(Map<String, Object> body) {
        return body.entrySet().stream()
                .map(entry -> "\"" + escape(entry.getKey()) + "\":" + jsonValue(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String jsonValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
