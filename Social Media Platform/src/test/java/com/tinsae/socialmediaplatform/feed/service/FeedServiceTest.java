package com.tinsae.socialmediaplatform.feed.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaType;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import com.tinsae.socialmediaplatform.follow.entity.Follow;
import com.tinsae.socialmediaplatform.follow.repository.FollowRepository;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.service.MediaService;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.reaction.entity.Reaction;
import com.tinsae.socialmediaplatform.reaction.repository.ReactionRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FeedService feedService;

    @Test
    void getFeedReturnsPostsForViewerAndFollowedUsers() {
        User viewer = TestDataFactory.user("viewer");
        User followed = TestDataFactory.user("followed");
        Follow follow = TestDataFactory.follow(viewer, followed);
        Post post = TestDataFactory.post(followed);
        Media media = media(post, followed);
        Reaction reaction = reaction(viewer, post);
        var jwt = TestDataFactory.jwt(viewer);

        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(followRepository.findByFollowerIdAndStatus(viewer.getId(), FollowStatus.ACCEPTED))
                .thenReturn(List.of(follow));
        when(postRepository.findFeedPosts(
                eq(viewer.getId()),
                any(),
                eq(PostStatus.PUBLISHED),
                eq(PostVisibility.PUBLIC),
                eq(PostVisibility.FOLLOWERS_ONLY),
                eq(null),
                any(Pageable.class)
        )).thenReturn(List.of(post));
        when(mediaService.findMediaForPost(post.getId())).thenReturn(List.of(media));
        when(reactionRepository.findByUserIdAndPostId(viewer.getId(), post.getId()))
                .thenReturn(Optional.of(reaction));

        var response = feedService.getFeed(null, 10, jwt);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(post.getId());
        assertThat(response.items().getFirst().media()).hasSize(1);
        assertThat(response.items().getFirst().viewerReaction()).isEqualTo(ReactionType.LIKE);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void getFeedUsesDefaultLimitWhenLimitIsNull() {
        User viewer = TestDataFactory.user("viewer");
        var jwt = TestDataFactory.jwt(viewer);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(followRepository.findByFollowerIdAndStatus(viewer.getId(), FollowStatus.ACCEPTED))
                .thenReturn(List.of());
        when(postRepository.findFeedPosts(
                eq(viewer.getId()),
                any(),
                eq(PostStatus.PUBLISHED),
                eq(PostVisibility.PUBLIC),
                eq(PostVisibility.FOLLOWERS_ONLY),
                eq(null),
                pageableCaptor.capture()
        )).thenReturn(List.of());

        var response = feedService.getFeed(null, null, jwt);

        assertThat(response.items()).isEmpty();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
    }

    @Test
    void getFeedClampsLimitBelowOne() {
        User viewer = TestDataFactory.user("viewer");
        var jwt = TestDataFactory.jwt(viewer);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(followRepository.findByFollowerIdAndStatus(viewer.getId(), FollowStatus.ACCEPTED))
                .thenReturn(List.of());
        when(postRepository.findFeedPosts(
                eq(viewer.getId()),
                any(),
                eq(PostStatus.PUBLISHED),
                eq(PostVisibility.PUBLIC),
                eq(PostVisibility.FOLLOWERS_ONLY),
                eq(null),
                pageableCaptor.capture()
        )).thenReturn(List.of());

        feedService.getFeed(null, 0, jwt);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void getFeedClampsLimitAboveMaxAndAllowsMissingViewerReaction() {
        User viewer = TestDataFactory.user("viewer");
        Post post = TestDataFactory.post(viewer);
        var jwt = TestDataFactory.jwt(viewer);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(followRepository.findByFollowerIdAndStatus(viewer.getId(), FollowStatus.ACCEPTED))
                .thenReturn(List.of());
        when(postRepository.findFeedPosts(
                eq(viewer.getId()),
                any(),
                eq(PostStatus.PUBLISHED),
                eq(PostVisibility.PUBLIC),
                eq(PostVisibility.FOLLOWERS_ONLY),
                eq(null),
                pageableCaptor.capture()
        )).thenReturn(List.of(post));
        when(mediaService.findMediaForPost(post.getId())).thenReturn(List.of());
        when(reactionRepository.findByUserIdAndPostId(viewer.getId(), post.getId()))
                .thenReturn(Optional.empty());

        var response = feedService.getFeed(null, 101, jwt);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(101);
        assertThat(response.items().getFirst().viewerReaction()).isNull();
    }

    private Media media(Post post, User uploader) {
        Media media = new Media();
        media.setId(UUID.randomUUID());
        media.setUploader(uploader);
        media.setPost(post);
        media.setUrl("/uploads/image.png");
        media.setMediaType(MediaType.IMAGE);
        media.setMimeType("image/png");
        media.setSizeBytes(123L);
        media.setAltText("image");
        media.setCreatedAt(Instant.now());
        return media;
    }

    private Reaction reaction(User user, Post post) {
        Reaction reaction = new Reaction();
        reaction.setId(UUID.randomUUID());
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setReactionType(ReactionType.LIKE);
        reaction.setCreatedAt(Instant.now());
        return reaction;
    }
}
