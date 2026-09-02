package com.tinsae.socialmediaplatform.security.jwt;

import com.tinsae.socialmediaplatform.role.entity.UserRole;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtTokenService {

    private static final JwsHeader JWS_HEADER = JwsHeader.with(MacAlgorithm.HS256).build();

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = baseClaims(user, now, expiresAt)
                .claim("token_type", "access")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(JWS_HEADER, claims)).getTokenValue();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        JwtClaimsSet claims = baseClaims(user, now, expiresAt)
                .claim("token_type", "refresh")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(JWS_HEADER, claims)).getTokenValue();
    }

    private JwtClaimsSet.Builder baseClaims(User user, Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("roles", roleNames(user))
                .id(UUID.randomUUID().toString());
    }

    private Set<String> roleNames(User user) {
        return user.getRoles()
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .collect(Collectors.toSet());
    }
}
