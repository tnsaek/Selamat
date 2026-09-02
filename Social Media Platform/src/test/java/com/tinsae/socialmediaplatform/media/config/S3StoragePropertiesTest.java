package com.tinsae.socialmediaplatform.media.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3StoragePropertiesTest {

    @Test
    void accessorsReturnConfiguredValues() {
        S3StorageProperties properties = new S3StorageProperties();

        properties.setBucket("bucket");
        properties.setRegion("us-east-1");
        properties.setKeyPrefix("uploads");
        properties.setPublicBaseUrl("https://cdn.example.com");

        assertThat(properties.getBucket()).isEqualTo("bucket");
        assertThat(properties.getRegion()).isEqualTo("us-east-1");
        assertThat(properties.getKeyPrefix()).isEqualTo("uploads");
        assertThat(properties.getPublicBaseUrl()).isEqualTo("https://cdn.example.com");
    }
}
