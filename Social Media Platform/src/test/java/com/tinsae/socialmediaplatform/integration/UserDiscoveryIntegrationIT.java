package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.profile.repository.ProfileRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserDiscoveryIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void authenticatedUserSearchesActiveUsersAndLoadsUserDetails() throws Exception {
        String searchTerm = "discover" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String viewerUsername = searchTerm + "viewer";
        String firstUsername = searchTerm + "alpha";
        String secondUsername = searchTerm + "bravo";
        String suspendedUsername = searchTerm + "suspended";

        String viewerToken = extractAccessToken(signUp(viewerUsername, viewerUsername + "@example.com").body());
        signUp(firstUsername, firstUsername + "@example.com");
        signUp(secondUsername, secondUsername + "@example.com");
        signUp(suspendedUsername, suspendedUsername + "@example.com");

        User firstUser = userRepository.findByUsername(firstUsername).orElseThrow();
        User secondUser = userRepository.findByUsername(secondUsername).orElseThrow();
        User suspendedUser = userRepository.findByUsername(suspendedUsername).orElseThrow();
        updateProfile(firstUser, "Selamat " + searchTerm + " Alpha", "http://localhost:8080/uploads/profile-alpha.png");
        updateProfile(secondUser, "Selamat " + searchTerm + " Bravo", "http://localhost:8080/uploads/profile-bravo.png");
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(suspendedUser);

        HttpResponse<String> shortQueryResponse = get("/api/users/search?query=x&limit=10", viewerToken);

        assertThat(shortQueryResponse.statusCode()).isEqualTo(200);
        assertThat(shortQueryResponse.body()).isEqualTo("[]");

        HttpResponse<String> searchResponse = get("/api/users/search?query=%s&limit=20".formatted(searchTerm), viewerToken);

        assertThat(searchResponse.statusCode()).isEqualTo(200);
        assertThat(searchResponse.body())
                .contains("\"id\":\"" + firstUser.getId() + "\"")
                .contains("\"username\":\"" + firstUsername + "\"")
                .contains("\"displayName\":\"Selamat " + searchTerm + " Alpha\"")
                .contains("\"avatarUrl\":\"http://localhost:8080/uploads/profile-alpha.png\"")
                .contains("\"id\":\"" + secondUser.getId() + "\"")
                .contains("\"username\":\"" + secondUsername + "\"")
                .doesNotContain(suspendedUsername);

        HttpResponse<String> limitedSearchResponse = get("/api/users/search?query=%s&limit=1".formatted(searchTerm), viewerToken);

        assertThat(limitedSearchResponse.statusCode()).isEqualTo(200);
        assertThat(limitedSearchResponse.body())
                .contains("\"username\":\"" + firstUsername + "\"")
                .doesNotContain("\"username\":\"" + secondUsername + "\"");

        HttpResponse<String> currentUserResponse = get("/api/users/me", viewerToken);

        assertThat(currentUserResponse.statusCode()).isEqualTo(200);
        assertThat(currentUserResponse.body())
                .contains("\"username\":\"" + viewerUsername + "\"")
                .contains("\"email\":\"" + viewerUsername + "@example.com\"")
                .contains("\"status\":\"ACTIVE\"")
                .contains("\"roles\":[\"USER\"]");

        HttpResponse<String> userByIdResponse = get("/api/users/%s".formatted(firstUser.getId()), viewerToken);

        assertThat(userByIdResponse.statusCode()).isEqualTo(200);
        assertThat(userByIdResponse.body())
                .contains("\"id\":\"" + firstUser.getId() + "\"")
                .contains("\"username\":\"" + firstUsername + "\"")
                .contains("\"email\":\"" + firstUsername + "@example.com\"");

        HttpResponse<String> suspendedUserResponse = get("/api/users/%s".formatted(suspendedUser.getId()), viewerToken);

        assertThat(suspendedUserResponse.statusCode()).isEqualTo(404);
    }

    private void updateProfile(User user, String displayName, String avatarUrl) {
        Profile profile = profileRepository.findByUserId(user.getId()).orElseThrow();
        profile.setDisplayName(displayName);
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);
    }
}
