package com.tinsae.socialmediaplatform.common.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void accessorsReturnValues() {
        ErrorResponse response = new ErrorResponse("CODE", "Message", List.of("detail"));

        assertThat(response.code()).isEqualTo("CODE");
        assertThat(response.message()).isEqualTo("Message");
        assertThat(response.details()).containsExactly("detail");
    }
}
