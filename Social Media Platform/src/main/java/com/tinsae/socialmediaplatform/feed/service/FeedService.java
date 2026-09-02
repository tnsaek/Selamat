package com.tinsae.socialmediaplatform.feed.service;

import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import com.tinsae.socialmediaplatform.common.mapper.MediaMapper;
import com.tinsae.socialmediaplatform.common.mapper.PostMapper;
import com.tinsae.socialmediaplatform.common.util.CursorUtils;
import com.tinsae.socialmediaplatform.feed.dto.FeedResponse;
import com.tinsae.socialmediaplatform.follow.entity.Follow;
import com.tinsae.socialmediaplatform.follow.repository.FollowRepository;
import com.tinsae.socialmediaplatform.media.dto.MediaResponse;
import com.tinsae.socialmediaplatform.media.service.MediaService;
import com.tinsae.socialmediaplatform.post.dto.PostResponse;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.reaction.repository.ReactionRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FeedService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final FollowRepository followRepository;
    private final MediaService mediaService;
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final UserService userService;

    public FeedService(
            FollowRepository followRepository,
            MediaService mediaService,
            PostRepository postRepository,
            ReactionRepository reactionRepository,
            UserService userService
    ) {
        this.followRepository = followRepository;
        this.mediaService = mediaService;
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(String cursor, Integer limit, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        int pageSize = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Instant cursorInstant = CursorUtils.parseCursor(cursor);

        List<UUID> followedAuthorIds = new ArrayList<>(followRepository.findByFollowerIdAndStatus(
                        currentUser.getId(),
                        FollowStatus.ACCEPTED
                )
                .stream()
                .map(Follow::getFollowed)
                .map(User::getId)
                .toList());
        if (followedAuthorIds.isEmpty()) {
            followedAuthorIds.add(currentUser.getId());
        }

        List<Post> feedPosts = new ArrayList<>(postRepository.findFeedPosts(
                currentUser.getId(),
                followedAuthorIds,
                PostStatus.PUBLISHED,
                PostVisibility.PUBLIC,
                PostVisibility.FOLLOWERS_ONLY,
                cursorInstant,
                pageable
        ));
        String nextCursor = CursorUtils.trimAndNextCursor(feedPosts, pageSize, Post::getCreatedAt);

        List<PostResponse> posts = feedPosts.stream()
                .map(post -> toFeedPost(post, currentUser.getId()))
                .toList();

        return new FeedResponse(posts, nextCursor);
    }

    private PostResponse toFeedPost(Post post, UUID viewerId) {
        List<MediaResponse> media = mediaService.findMediaForPost(post.getId())
                .stream()
                .map(MediaMapper::toResponse)
                .toList();

        ReactionType viewerReaction = reactionRepository.findByUserIdAndPostId(viewerId, post.getId())
                .map(reaction -> reaction.getReactionType())
                .orElse(null);

        return PostMapper.toResponse(post, media, viewerReaction);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
