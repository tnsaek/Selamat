package com.tinsae.socialmediaplatform.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRequestTest {

    @Test
    void refreshTokenAccessorReturnsValue() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        assertThat(request.refreshToken()).isEqualTo("refresh-token");
    }
}
