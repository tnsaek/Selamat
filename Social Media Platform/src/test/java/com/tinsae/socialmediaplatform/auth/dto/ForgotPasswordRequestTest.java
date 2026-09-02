package com.tinsae.socialmediaplatform.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForgotPasswordRequestTest {

    @Test
    void storesEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("tinsae@example.com");

        assertThat(request.email()).isEqualTo("tinsae@example.com");
    }
}
