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

class PostVisibilityIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void feedAndPostDetailsRespectPublicFollowersOnlyAndPrivateVisibility() throws Exception {
        String authorUsername = uniqueUsername("visauthor");
        String viewerUsername = uniqueUsername("visviewer");
        String outsiderUsername = uniqueUsername("visoutside");
        String authorToken = extractAccessToken(signUp(authorUsername, authorUsername + "@example.com").body());
        String viewerToken = extractAccessToken(signUp(viewerUsername, viewerUsername + "@example.com").body());
        String outsiderToken = extractAccessToken(signUp(outsiderUsername, outsiderUsername + "@example.com").body());
        User author = userRepository.findByUsername(authorUsername).orElseThrow();

        Post publicPost = createPost(authorToken, "Public visibility post " + UUID.randomUUID(), "PUBLIC");
        Post followersPost = createPost(authorToken, "Followers visibility post " + UUID.randomUUID(), "FOLLOWERS_ONLY");
        Post privatePost = createPost(authorToken, "Private visibility post " + UUID.randomUUID(), "PRIVATE");

        HttpResponse<String> viewerFeedBeforeFollow = get("/api/feed?limit=20", viewerToken);

        assertThat(viewerFeedBeforeFollow.statusCode()).isEqualTo(200);
        assertThat(viewerFeedBeforeFollow.body())
                .contains("\"id\":\"" + publicPost.getId() + "\"")
                .doesNotContain("\"id\":\"" + followersPost.getId() + "\"")
                .doesNotContain("\"id\":\"" + privatePost.getId() + "\"");

        HttpResponse<String> blockedFollowersPostResponse = get("/api/posts/%s".formatted(followersPost.getId()), viewerToken);
        HttpResponse<String> blockedPrivatePostResponse = get("/api/posts/%s".formatted(privatePost.getId()), viewerToken);

        assertThat(blockedFollowersPostResponse.statusCode()).isEqualTo(404);
        assertThat(blockedPrivatePostResponse.statusCode()).isEqualTo(404);

        HttpResponse<String> followResponse = postJson("/api/users/%s/follow".formatted(author.getId()), "{}", viewerToken);
        assertThat(followResponse.statusCode()).isEqualTo(201);

        HttpResponse<String> viewerFeedAfterFollow = get("/api/feed?limit=20", viewerToken);

        assertThat(viewerFeedAfterFollow.statusCode()).isEqualTo(200);
        assertThat(viewerFeedAfterFollow.body())
                .contains("\"id\":\"" + publicPost.getId() + "\"")
                .contains("\"id\":\"" + followersPost.getId() + "\"")
                .doesNotContain("\"id\":\"" + privatePost.getId() + "\"");

        HttpResponse<String> allowedFollowersPostResponse = get("/api/posts/%s".formatted(followersPost.getId()), viewerToken);
        assertThat(allowedFollowersPostResponse.statusCode()).isEqualTo(200);
        assertThat(allowedFollowersPostResponse.body())
                .contains("\"id\":\"" + followersPost.getId() + "\"")
                .contains("\"visibility\":\"FOLLOWERS_ONLY\"");

        HttpResponse<String> authorFeedResponse = get("/api/feed?limit=20", authorToken);

        assertThat(authorFeedResponse.statusCode()).isEqualTo(200);
        assertThat(authorFeedResponse.body())
                .contains("\"id\":\"" + publicPost.getId() + "\"")
                .contains("\"id\":\"" + followersPost.getId() + "\"")
                .contains("\"id\":\"" + privatePost.getId() + "\"");

        HttpResponse<String> authorPrivatePostResponse = get("/api/posts/%s".formatted(privatePost.getId()), authorToken);
        assertThat(authorPrivatePostResponse.statusCode()).isEqualTo(200);
        assertThat(authorPrivatePostResponse.body())
                .contains("\"id\":\"" + privatePost.getId() + "\"")
                .contains("\"visibility\":\"PRIVATE\"");

        HttpResponse<String> outsiderPrivatePostResponse = get("/api/posts/%s".formatted(privatePost.getId()), outsiderToken);
        assertThat(outsiderPrivatePostResponse.statusCode()).isEqualTo(404);
    }

    @Test
    void authorUpdatesAndDeletesOwnPostButOtherUsersCannotModifyIt() throws Exception {
        String authorUsername = uniqueUsername("postauthor");
        String viewerUsername = uniqueUsername("postviewer");
        String authorToken = extractAccessToken(signUp(authorUsername, authorUsername + "@example.com").body());
        String viewerToken = extractAccessToken(signUp(viewerUsername, viewerUsername + "@example.com").body());

        Post post = createPost(authorToken, "Editable post " + UUID.randomUUID(), "PUBLIC");

        HttpResponse<String> forbiddenUpdateResponse = patchJson("/api/posts/%s".formatted(post.getId()), """
                {
                  "content": "Viewer should not change this"
                }
                """, viewerToken);

        assertThat(forbiddenUpdateResponse.statusCode()).isEqualTo(403);

        HttpResponse<String> updateResponse = patchJson("/api/posts/%s".formatted(post.getId()), """
                {
                  "content": "Updated integration post",
                  "visibility": "PRIVATE"
                }
                """, authorToken);

        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.body())
                .contains("\"content\":\"Updated integration post\"")
                .contains("\"visibility\":\"PRIVATE\"");
        Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getContent()).isEqualTo("Updated integration post");
        assertThat(updatedPost.getVisibility()).isEqualTo(PostVisibility.PRIVATE);

        HttpResponse<String> forbiddenDeleteResponse = delete("/api/posts/%s".formatted(post.getId()), viewerToken);
        assertThat(forbiddenDeleteResponse.statusCode()).isEqualTo(403);

        HttpResponse<String> deleteResponse = delete("/api/posts/%s".formatted(post.getId()), authorToken);

        assertThat(deleteResponse.statusCode()).isEqualTo(204);
        assertThat(postRepository.findById(post.getId()).orElseThrow().getStatus()).isEqualTo(PostStatus.DELETED);

        HttpResponse<String> deletedPostResponse = get("/api/posts/%s".formatted(post.getId()), authorToken);
        assertThat(deletedPostResponse.statusCode()).isEqualTo(404);
    }

    private Post createPost(String accessToken, String content, String visibility) throws Exception {
        HttpResponse<String> response = postJson("/api/posts", """
                {
                  "content": "%s",
                  "visibility": "%s",
                  "mediaIds": []
                }
                """.formatted(content, visibility), accessToken);

        assertThat(response.statusCode()).isEqualTo(201);
        return postRepository.findAll()
                .stream()
                .filter(post -> content.equals(post.getContent()))
                .findFirst()
                .orElseThrow();
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
