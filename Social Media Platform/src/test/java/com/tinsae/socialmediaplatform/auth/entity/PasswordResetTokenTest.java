package com.tinsae.socialmediaplatform.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenTest {

    @Test
    void isUsedReturnsTrueWhenUsedAtExists() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsedAt(Instant.now());

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void isExpiredComparesExpiresAtWithCurrentTime() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(Instant.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void prePersistSetsCreatedAtWhenMissing() {
        PasswordResetToken token = new PasswordResetToken();

        token.prePersist();

        assertThat(token.getCreatedAt()).isNotNull();
    }
}
