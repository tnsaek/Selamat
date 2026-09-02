package com.tinsae.socialmediaplatform.auth.entity;

import com.tinsae.socialmediaplatform.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void accessorsAndStatusHelpersReflectState() {
        RefreshToken refreshToken = new RefreshToken();
        var user = TestDataFactory.user("user");
        Instant createdAt = Instant.now();

        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash("hash");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(60));
        refreshToken.setCreatedAt(createdAt);

        assertThat(refreshToken.getId()).isNotNull();
        assertThat(refreshToken.getUser()).isSameAs(user);
        assertThat(refreshToken.getTokenHash()).isEqualTo("hash");
        assertThat(refreshToken.getCreatedAt()).isEqualTo(createdAt);
        assertThat(refreshToken.isRevoked()).isFalse();
        assertThat(refreshToken.isExpired()).isFalse();

        refreshToken.setRevokedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().minusSeconds(60));

        assertThat(refreshToken.getRevokedAt()).isNotNull();
        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.isExpired()).isTrue();
    }
}
