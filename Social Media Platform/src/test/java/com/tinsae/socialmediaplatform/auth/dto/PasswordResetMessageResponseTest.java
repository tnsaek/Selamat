package com.tinsae.socialmediaplatform.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetMessageResponseTest {

    @Test
    void storesMessage() {
        PasswordResetMessageResponse response = new PasswordResetMessageResponse("Message");

        assertThat(response.message()).isEqualTo("Message");
    }
}
