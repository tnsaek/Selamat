package com.tinsae.socialmediaplatform.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppMailPropertiesTest {

    @Test
    void storesMailSettings() {
        AppMailProperties properties = new AppMailProperties();

        properties.setEnabled(true);
        properties.setFrom("no-reply@example.com");
        properties.setFrontendResetPasswordUrl("https://app.example.com/reset-password");

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getFrom()).isEqualTo("no-reply@example.com");
        assertThat(properties.getFrontendResetPasswordUrl()).isEqualTo("https://app.example.com/reset-password");
    }
}
