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
import java.util.Map;

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

        Instant now = Instant.now();

        var claims = JwtClaimsSet.builder()
                .issuer("erp-auth-service")
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES))
                .subject(userId)
                .claim("preferred_username", username)
                .claim("sid", sessionId)
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
