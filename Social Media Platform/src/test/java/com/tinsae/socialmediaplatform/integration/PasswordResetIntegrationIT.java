package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.auth.entity.PasswordResetToken;
import com.tinsae.socialmediaplatform.auth.repository.PasswordResetTokenRepository;
import com.tinsae.socialmediaplatform.auth.repository.RefreshTokenRepository;
import com.tinsae.socialmediaplatform.common.service.EmailService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private CapturingEmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearCapturedEmails() {
        emailService.clear();
    }

    @Test
    void forgotAndResetPasswordUpdatesCredentialsAndRevokesRefreshTokens() throws Exception {
        String username = "reset" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@example.com";
        signUp(username, email, "OldPassword123");
        User user = userRepository.findByUsername(username).orElseThrow();
        login(email, "OldPassword123");

        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).hasSize(2);

        HttpResponse<String> forgotPasswordResponse = postJson("/api/auth/forgot-password", """
                {
                  "email": "%s"
                }
                """.formatted(email.toUpperCase()));

        assertThat(forgotPasswordResponse.statusCode()).isEqualTo(200);
        assertThat(forgotPasswordResponse.body())
                .contains("\"message\":\"If an account exists for this email, a reset link has been sent.\"");

        PasswordResetToken savedResetToken = passwordResetTokenRepository.findAll()
                .stream()
                .filter(candidate -> candidate.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(savedResetToken.getUsedAt()).isNull();
        assertThat(savedResetToken.getExpiresAt()).isAfter(savedResetToken.getCreatedAt());

        String resetLink = emailService.resetLinkFor(email);
        assertThat(resetLink).startsWith("http://localhost:4200/reset-password?token=");
        String plaintextResetToken = extractTokenFromResetLink(resetLink);

        HttpResponse<String> resetPasswordResponse = postJson("/api/auth/reset-password", """
                {
                  "token": "%s",
                  "newPassword": "NewPassword123"
                }
                """.formatted(plaintextResetToken));

        assertThat(resetPasswordResponse.statusCode()).isEqualTo(200);
        assertThat(resetPasswordResponse.body())
                .contains("\"message\":\"Password has been reset successfully. Log in with your new password.\"");

        PasswordResetToken usedResetToken = passwordResetTokenRepository.findById(savedResetToken.getId()).orElseThrow();
        assertThat(usedResetToken.getUsedAt()).isNotNull();
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();

        HttpResponse<String> oldPasswordLoginResponse = login(email, "OldPassword123");
        assertThat(oldPasswordLoginResponse.statusCode()).isEqualTo(401);

        HttpResponse<String> newPasswordLoginResponse = login(email, "NewPassword123");
        assertThat(newPasswordLoginResponse.statusCode()).isEqualTo(200);
        assertThat(extractAccessToken(newPasswordLoginResponse.body())).isNotBlank();
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).hasSize(1);
    }

    @Test
    void forgotPasswordDoesNotRevealMissingAccounts() throws Exception {
        String email = "missing-" + UUID.randomUUID() + "@example.com";

        HttpResponse<String> response = postJson("/api/auth/forgot-password", """
                {
                  "email": "%s"
                }
                """.formatted(email));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"message\":\"If an account exists for this email, a reset link has been sent.\"");
        assertThat(emailService.hasResetLinkFor(email)).isFalse();
    }

    private String extractTokenFromResetLink(String resetLink) {
        String token = resetLink.substring(resetLink.indexOf("token=") + "token=".length());
        return URLDecoder.decode(token, StandardCharsets.UTF_8);
    }

    @TestConfiguration
    static class PasswordResetIntegrationTestConfiguration {

        @Bean
        @Primary
        CapturingEmailService capturingEmailService() {
            return new CapturingEmailService();
        }
    }

    static class CapturingEmailService implements EmailService {

        private final Map<String, String> resetLinksByEmail = new ConcurrentHashMap<>();

        @Override
        public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
            resetLinksByEmail.put(recipientEmail.toLowerCase(), resetLink);
        }

        String resetLinkFor(String email) {
            return resetLinksByEmail.get(email.toLowerCase());
        }

        boolean hasResetLinkFor(String email) {
            return resetLinksByEmail.containsKey(email.toLowerCase());
        }

        void clear() {
            resetLinksByEmail.clear();
        }
    }
}
