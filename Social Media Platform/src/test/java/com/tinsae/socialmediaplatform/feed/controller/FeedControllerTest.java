package com.tinsae.socialmediaplatform.feed.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.feed.dto.FeedResponse;
import com.tinsae.socialmediaplatform.feed.service.FeedService;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedControllerTest {

    @Mock
    private FeedService feedService;

    @InjectMocks
    private FeedController feedController;

    @Test
    void getFeedDelegatesToFeedService() {
        User user = TestDataFactory.user("viewer");
        var jwt = TestDataFactory.jwt(user);
        FeedResponse feed = new FeedResponse(List.of(), null);
        when(feedService.getFeed("cursor", 25, jwt)).thenReturn(feed);

        FeedResponse response = feedController.getFeed("cursor", 25, jwt);

        assertThat(response).isSameAs(feed);
        verify(feedService).getFeed("cursor", 25, jwt);
    }
}
