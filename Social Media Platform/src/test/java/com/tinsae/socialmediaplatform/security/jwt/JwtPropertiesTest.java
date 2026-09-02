package com.tinsae.socialmediaplatform.security.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    @Test
    void accessorsReturnConfiguredValues() {
        JwtProperties properties = new JwtProperties();

        properties.setIssuer("selamat-api");
        properties.setSecret("secret");
        properties.setAccessTokenExpirationMinutes(15);
        properties.setRefreshTokenExpirationDays(30);

        assertThat(properties.getIssuer()).isEqualTo("selamat-api");
        assertThat(properties.getSecret()).isEqualTo("secret");
        assertThat(properties.getAccessTokenExpirationMinutes()).isEqualTo(15);
        assertThat(properties.getRefreshTokenExpirationDays()).isEqualTo(30);
    }
}
