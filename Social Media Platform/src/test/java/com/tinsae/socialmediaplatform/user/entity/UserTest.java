package com.tinsae.socialmediaplatform.user.entity;

import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void noArgsConstructorSetsDefaults() {
        User user = new User();

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void prePersistSetsMissingTimestamps() {
        User user = new User();

        user.prePersist();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void prePersistPreservesExistingTimestamps() {
        User user = new User();
        Instant createdAt = Instant.parse("2026-07-30T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-07-30T10:01:00Z");
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        user.prePersist();

        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void preUpdateRefreshesUpdatedAt() {
        User user = new User();
        Instant oldUpdatedAt = Instant.parse("2026-07-30T10:00:00Z");
        user.setUpdatedAt(oldUpdatedAt);

        user.preUpdate();

        assertThat(user.getUpdatedAt()).isAfter(oldUpdatedAt);
    }
}
