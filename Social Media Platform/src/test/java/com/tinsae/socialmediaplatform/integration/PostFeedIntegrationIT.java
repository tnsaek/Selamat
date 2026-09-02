package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostFeedIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void authenticatedUserCreatesPostAndSeesItInFeed() throws Exception {
        String username = "feeduser" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@example.com";
        HttpResponse<String> signupResponse = signUp(username, email);
        String accessToken = extractAccessToken(signupResponse.body());
        String content = "Integration feed post " + UUID.randomUUID();

        HttpResponse<String> createPostResponse = postJson("/api/posts", """
                {
                  "content": "%s",
                  "visibility": "PUBLIC",
                  "mediaIds": []
                }
                """.formatted(content), accessToken);

        assertThat(createPostResponse.statusCode()).isEqualTo(201);
        assertThat(createPostResponse.body())
                .contains("\"content\":\"" + content + "\"")
                .contains("\"visibility\":\"PUBLIC\"")
                .contains("\"status\":\"PUBLISHED\"");

        User author = userRepository.findByUsername(username).orElseThrow();
        Post savedPost = postRepository.findAll()
                .stream()
                .filter(post -> post.getAuthor().getId().equals(author.getId()))
                .filter(post -> content.equals(post.getContent()))
                .findFirst()
                .orElseThrow();

        assertThat(savedPost.getVisibility()).isEqualTo(PostVisibility.PUBLIC);
        assertThat(savedPost.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(savedPost.getCommentCount()).isZero();
        assertThat(savedPost.getReactionCount()).isZero();

        HttpResponse<String> feedResponse = get("/api/feed?limit=10", accessToken);

        assertThat(feedResponse.statusCode()).isEqualTo(200);
        assertThat(feedResponse.body())
                .contains("\"items\"")
                .contains("\"id\":\"" + savedPost.getId() + "\"")
                .contains("\"content\":\"" + content + "\"")
                .contains("\"username\":\"" + username + "\"");
    }
}
