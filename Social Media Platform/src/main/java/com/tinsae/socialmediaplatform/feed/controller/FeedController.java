package com.tinsae.socialmediaplatform.feed.controller;

import com.tinsae.socialmediaplatform.feed.dto.FeedResponse;
import com.tinsae.socialmediaplatform.feed.service.FeedService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public FeedResponse getFeed(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return feedService.getFeed(cursor, limit, jwt);
    }
}
