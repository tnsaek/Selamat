package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.auth.repository.RefreshTokenRepository;
import com.tinsae.socialmediaplatform.profile.repository.ProfileRepository;
import com.tinsae.socialmediaplatform.role.repository.UserRoleRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void signupCreatesUserProfileRoleAndRefreshToken() throws Exception {
        String username = "ituser" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@example.com";

        HttpResponse<String> response = signUp(username, email);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("\"accessToken\"")
                .contains("\"refreshToken\"")
                .contains("\"tokenType\":\"Bearer\"")
                .contains("\"username\":\"" + username + "\"")
                .contains("\"email\":\"" + email + "\"");

        User savedUser = userRepository.findByUsername(username).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(profileRepository.findByUserId(savedUser.getId())).isPresent();
        assertThat(userRoleRepository.findByIdUserId(savedUser.getId()))
                .extracting(userRole -> userRole.getRole().getName())
                .contains("USER");
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(savedUser.getId())).hasSize(1);
    }

    @Test
    void loginRefreshLogoutLifecycleRotatesAndRevokesRefreshTokens() throws Exception {
        String username = "ituser" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@example.com";
        signUp(username, email);
        User savedUser = userRepository.findByUsername(username).orElseThrow();

        HttpResponse<String> loginResponse = login(email, "StrongPassword123");

        assertThat(loginResponse.statusCode()).isEqualTo(200);
        String accessToken = extractAccessToken(loginResponse.body());
        String refreshToken = extractRefreshToken(loginResponse.body());
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(savedUser.getId())).hasSize(2);

        HttpResponse<String> meResponse = get("/api/users/me", accessToken);

        assertThat(meResponse.statusCode()).isEqualTo(200);
        assertThat(meResponse.body())
                .contains("\"username\":\"" + username + "\"")
                .contains("\"email\":\"" + email + "\"");

        HttpResponse<String> refreshResponse = postJson("/api/auth/refresh", """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken));

        assertThat(refreshResponse.statusCode()).isEqualTo(200);
        String rotatedAccessToken = extractAccessToken(refreshResponse.body());
        assertThat(rotatedAccessToken).isNotBlank();
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(savedUser.getId())).hasSize(2);

        HttpResponse<String> logoutResponse = postJson("/api/auth/logout", "{}", rotatedAccessToken);

        assertThat(logoutResponse.statusCode()).isEqualTo(204);
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(savedUser.getId())).isEmpty();
    }
}
