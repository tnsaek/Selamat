package com.tinsae.socialmediaplatform.feed.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedResponseTest {

    @Test
    void accessorsReturnValues() {
        FeedResponse response = new FeedResponse(List.of(), "next");

        assertThat(response.items()).isEmpty();
        assertThat(response.nextCursor()).isEqualTo("next");
    }
}
