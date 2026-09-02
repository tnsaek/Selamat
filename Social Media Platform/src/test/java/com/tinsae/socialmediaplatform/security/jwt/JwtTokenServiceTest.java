package com.tinsae.socialmediaplatform.security.jwt;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

    private JwtEncoder jwtEncoder;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("selamat-api");
        jwtProperties.setSecret("test-secret");
        jwtProperties.setAccessTokenExpirationMinutes(15);
        jwtProperties.setRefreshTokenExpirationDays(30);

        jwtEncoder = mock(JwtEncoder.class);
        jwtTokenService = new JwtTokenService(jwtEncoder, jwtProperties);
    }

    @Test
    void generateAccessTokenEncodesAccessClaimsWithHs256Header() {
        User user = TestDataFactory.user("tinsae");
        TestDataFactory.userRole(user, TestDataFactory.role("USER"));
        when(jwtEncoder.encode(org.mockito.ArgumentMatchers.any(JwtEncoderParameters.class)))
                .thenReturn(jwt("access-token"));

        String token = jwtTokenService.generateAccessToken(user);

        JwtEncoderParameters parameters = capturedParameters();
        assertThat(token).isEqualTo("access-token");
        assertThat(parameters.getJwsHeader().getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
        String issuer = parameters.getClaims().getClaim("iss");
        assertThat(parameters.getClaims().getSubject()).isEqualTo(user.getId().toString());
        String username = parameters.getClaims().getClaim("username");
        Set<String> roles = parameters.getClaims().getClaim("roles");
        String tokenType = parameters.getClaims().getClaim("token_type");
        assertThat(issuer).isEqualTo("selamat-api");
        assertThat(username).isEqualTo("tinsae");
        assertThat(roles).isEqualTo(Set.of("USER"));
        assertThat(tokenType).isEqualTo("access");
        assertThat(parameters.getClaims().getExpiresAt()).isAfter(parameters.getClaims().getIssuedAt());
        assertThat(parameters.getClaims().getId()).isNotBlank();
    }

    @Test
    void generateRefreshTokenEncodesRefreshClaims() {
        User user = TestDataFactory.user("tinsae");
        when(jwtEncoder.encode(org.mockito.ArgumentMatchers.any(JwtEncoderParameters.class)))
                .thenReturn(jwt("refresh-token"));

        String token = jwtTokenService.generateRefreshToken(user);

        JwtEncoderParameters parameters = capturedParameters();
        assertThat(token).isEqualTo("refresh-token");
        assertThat(parameters.getClaims().getSubject()).isEqualTo(user.getId().toString());
        Set<String> roles = parameters.getClaims().getClaim("roles");
        String tokenType = parameters.getClaims().getClaim("token_type");
        assertThat(roles).isEmpty();
        assertThat(tokenType).isEqualTo("refresh");
        assertThat(parameters.getClaims().getExpiresAt()).isAfter(parameters.getClaims().getIssuedAt());
    }

    private JwtEncoderParameters capturedParameters() {
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        org.mockito.Mockito.verify(jwtEncoder).encode(captor.capture());
        return captor.getValue();
    }

    private Jwt jwt(String tokenValue) {
        Instant now = Instant.now();
        return new Jwt(
                tokenValue,
                now,
                now.plusSeconds(900),
                Map.of("alg", "HS256"),
                Map.of("sub", "subject")
        );
    }
}
