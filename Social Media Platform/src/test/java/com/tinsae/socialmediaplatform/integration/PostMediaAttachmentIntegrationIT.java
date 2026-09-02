package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostMediaAttachmentIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void uploadedMediaCanBeAttachedToPostAndReturnedInPostAndFeedResponses() throws Exception {
        String username = uniqueUsername("mediaauthor");
        String accessToken = extractAccessToken(signUp(username, username + "@example.com").body());
        User author = userRepository.findByUsername(username).orElseThrow();

        HttpResponse<String> uploadResponse = postMultipart(
                "/api/media",
                "file",
                "post-image.png",
                "image/png",
                ("fake image content " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8),
                Map.of("altText", "Post image alt text"),
                accessToken
        );

        assertThat(uploadResponse.statusCode()).isEqualTo(201);
        String mediaId = extractStringField(uploadResponse.body(), "id");
        String mediaUrl = extractStringField(uploadResponse.body(), "url");

        String content = "Post with attached media " + UUID.randomUUID();
        HttpResponse<String> createPostResponse = postJson("/api/posts", """
                {
                  "content": "%s",
                  "visibility": "PUBLIC",
                  "mediaIds": ["%s"]
                }
                """.formatted(content, mediaId), accessToken);

        assertThat(createPostResponse.statusCode()).isEqualTo(201);
        assertThat(createPostResponse.body())
                .contains("\"content\":\"" + content + "\"")
                .contains("\"media\"")
                .contains("\"id\":\"" + mediaId + "\"")
                .contains("\"url\":\"" + mediaUrl + "\"")
                .contains("\"altText\":\"Post image alt text\"");

        Post post = postRepository.findAll()
                .stream()
                .filter(candidate -> content.equals(candidate.getContent()))
                .findFirst()
                .orElseThrow();
        Media media = mediaRepository.findById(UUID.fromString(mediaId)).orElseThrow();

        assertThat(post.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(media.getPost().getId()).isEqualTo(post.getId());

        HttpResponse<String> postDetailResponse = get("/api/posts/%s".formatted(post.getId()), accessToken);

        assertThat(postDetailResponse.statusCode()).isEqualTo(200);
        assertThat(postDetailResponse.body())
                .contains("\"id\":\"" + post.getId() + "\"")
                .contains("\"id\":\"" + mediaId + "\"")
                .contains("\"mediaType\":\"IMAGE\"")
                .contains("\"mimeType\":\"image/png\"");

        HttpResponse<String> feedResponse = get("/api/feed?limit=10", accessToken);

        assertThat(feedResponse.statusCode()).isEqualTo(200);
        assertThat(feedResponse.body())
                .contains("\"id\":\"" + post.getId() + "\"")
                .contains("\"id\":\"" + mediaId + "\"")
                .contains("\"url\":\"" + mediaUrl + "\"");
    }

    @Test
    void postCreationRejectsDuplicateOrOtherUsersMediaIds() throws Exception {
        String ownerUsername = uniqueUsername("mediaowner");
        String otherUsername = uniqueUsername("mediaother");
        String ownerToken = extractAccessToken(signUp(ownerUsername, ownerUsername + "@example.com").body());
        String otherToken = extractAccessToken(signUp(otherUsername, otherUsername + "@example.com").body());

        String mediaId = extractStringField(postMultipart(
                "/api/media",
                "file",
                "owned-image.png",
                "image/png",
                "owned fake image".getBytes(StandardCharsets.UTF_8),
                Map.of(),
                ownerToken
        ).body(), "id");

        HttpResponse<String> duplicateMediaResponse = postJson("/api/posts", """
                {
                  "content": "Duplicate media IDs",
                  "visibility": "PUBLIC",
                  "mediaIds": ["%s", "%s"]
                }
                """.formatted(mediaId, mediaId), ownerToken);

        assertThat(duplicateMediaResponse.statusCode()).isEqualTo(422);
        assertThat(duplicateMediaResponse.body()).contains("Media IDs must not contain duplicates.");

        HttpResponse<String> otherUserMediaResponse = postJson("/api/posts", """
                {
                  "content": "Other user media",
                  "visibility": "PUBLIC",
                  "mediaIds": ["%s"]
                }
                """.formatted(mediaId), otherToken);

        assertThat(otherUserMediaResponse.statusCode()).isEqualTo(403);
        assertThat(otherUserMediaResponse.body()).contains("You can only attach media that you uploaded.");
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
