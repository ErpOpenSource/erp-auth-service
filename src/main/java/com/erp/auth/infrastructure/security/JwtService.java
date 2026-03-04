package com.erp.auth.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpirationMinutes;

    public JwtService(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.access-token-expiration-minutes}") long expirationMinutes
    ) {
        this.accessTokenExpirationMinutes = expirationMinutes;

        var key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    public String generateAccessToken(
            String userId,
            String username,
            String sessionId
    ) {
        return generateAccessToken(userId, username, sessionId, List.of(), List.of(), List.of(), List.of());
    }

    public String generateAccessToken(
            String userId,
            String username,
            String sessionId,
            List<String> roles
    ) {
        return generateAccessToken(userId, username, sessionId, roles, List.of(), List.of(), List.of());
    }

    public String generateAccessToken(
            String userId,
            String username,
            String sessionId,
            List<String> roles,
            List<String> modules,
            List<String> departments,
            List<String> permissions
    ) {

        Instant now = Instant.now();
        List<String> safeRoles = roles == null ? List.of() : roles;
        List<String> safeModules = modules == null ? List.of() : modules;
        List<String> safeDepartments = departments == null ? List.of() : departments;
        List<String> safePermissions = permissions == null ? List.of() : permissions;

        var claims = JwtClaimsSet.builder()
                .issuer("erp-auth-service")
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES))
                .subject(userId)
                .claim("preferred_username", username)
                .claim("sid", sessionId)
                .claim("roles", safeRoles)
                .claim("modules", safeModules)
                .claim("departments", safeDepartments)
                .claim("permissions", safePermissions)
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
