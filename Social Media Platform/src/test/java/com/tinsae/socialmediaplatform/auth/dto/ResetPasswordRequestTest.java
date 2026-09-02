package com.tinsae.socialmediaplatform.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResetPasswordRequestTest {

    @Test
    void storesTokenAndPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "StrongPassword123");

        assertThat(request.token()).isEqualTo("token");
        assertThat(request.newPassword()).isEqualTo("StrongPassword123");
    }
}
