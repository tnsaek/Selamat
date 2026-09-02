package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.common.enums.MediaStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaType;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.profile.repository.ProfileRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileMediaIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void userUploadsProfileAndCoverImagesThenUpdatesProfile() throws Exception {
        String username = "profile" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@example.com";
        String accessToken = extractAccessToken(signUp(username, email).body());
        User user = userRepository.findByUsername(username).orElseThrow();

        HttpResponse<String> profileUploadResponse = uploadImage(
                "profile-picture.png",
                "Profile picture",
                accessToken
        );
        HttpResponse<String> coverUploadResponse = uploadImage(
                "cover-image.png",
                "Cover image",
                accessToken
        );

        assertThat(profileUploadResponse.statusCode()).isEqualTo(201);
        assertThat(coverUploadResponse.statusCode()).isEqualTo(201);

        String profileImageUrl = extractStringField(profileUploadResponse.body(), "url");
        String coverImageUrl = extractStringField(coverUploadResponse.body(), "url");

        assertThat(profileUploadResponse.body())
                .contains("\"mediaType\":\"IMAGE\"")
                .contains("\"mimeType\":\"image/png\"")
                .contains("\"altText\":\"Profile picture\"");
        assertThat(coverUploadResponse.body())
                .contains("\"mediaType\":\"IMAGE\"")
                .contains("\"mimeType\":\"image/png\"")
                .contains("\"altText\":\"Cover image\"");

        assertThat(mediaRepository.findAll())
                .filteredOn(media -> media.getUploader().getId().equals(user.getId()))
                .hasSize(2)
                .allSatisfy(media -> {
                    assertThat(media.getMediaType()).isEqualTo(MediaType.IMAGE);
                    assertThat(media.getMimeType()).isEqualTo("image/png");
                    assertThat(media.getStatus()).isEqualTo(MediaStatus.VISIBLE);
                    assertThat(media.getSizeBytes()).isPositive();
                    assertThat(media.getUrl()).startsWith("http://localhost:8080/uploads/");
                });

        HttpResponse<String> updateProfileResponse = patchJson("/api/users/%s/profile".formatted(user.getId()), """
                {
                  "displayName": "Integration Profile",
                  "bio": "Profile updated through integration test.",
                  "avatarUrl": "%s",
                  "coverImageUrl": "%s",
                  "street": "123 Test Street",
                  "city": "Addis Ababa",
                  "state": "Addis Ababa",
                  "country": "Ethiopia",
                  "websiteUrl": "https://example.com"
                }
                """.formatted(profileImageUrl, coverImageUrl), accessToken);

        assertThat(updateProfileResponse.statusCode()).isEqualTo(200);
        assertThat(updateProfileResponse.body())
                .contains("\"displayName\":\"Integration Profile\"")
                .contains("\"avatarUrl\":\"" + profileImageUrl + "\"")
                .contains("\"coverImageUrl\":\"" + coverImageUrl + "\"")
                .contains("\"street\":\"123 Test Street\"")
                .contains("\"city\":\"Addis Ababa\"")
                .contains("\"state\":\"Addis Ababa\"")
                .contains("\"country\":\"Ethiopia\"");

        Profile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getAvatarUrl()).isEqualTo(profileImageUrl);
        assertThat(profile.getCoverImageUrl()).isEqualTo(coverImageUrl);
        assertThat(profile.getStreet()).isEqualTo("123 Test Street");
        assertThat(profile.getCity()).isEqualTo("Addis Ababa");
        assertThat(profile.getState()).isEqualTo("Addis Ababa");
        assertThat(profile.getCountry()).isEqualTo("Ethiopia");

        HttpResponse<String> getProfileResponse = get("/api/users/%s/profile".formatted(user.getId()), accessToken);

        assertThat(getProfileResponse.statusCode()).isEqualTo(200);
        assertThat(getProfileResponse.body())
                .contains("\"userId\":\"" + user.getId() + "\"")
                .contains("\"avatarUrl\":\"" + profileImageUrl + "\"")
                .contains("\"coverImageUrl\":\"" + coverImageUrl + "\"");
    }

    private HttpResponse<String> uploadImage(String filename, String altText, String accessToken) throws Exception {
        return postMultipart(
                "/api/media",
                "file",
                filename,
                "image/png",
                ("fake png content " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8),
                Map.of("altText", altText),
                accessToken
        );
    }
}
