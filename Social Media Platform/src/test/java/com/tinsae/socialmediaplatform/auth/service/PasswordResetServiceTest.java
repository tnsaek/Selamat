package com.tinsae.socialmediaplatform.auth.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.auth.dto.ForgotPasswordRequest;
import com.tinsae.socialmediaplatform.auth.dto.ResetPasswordRequest;
import com.tinsae.socialmediaplatform.auth.entity.PasswordResetToken;
import com.tinsae.socialmediaplatform.auth.entity.RefreshToken;
import com.tinsae.socialmediaplatform.auth.repository.PasswordResetTokenRepository;
import com.tinsae.socialmediaplatform.auth.repository.RefreshTokenRepository;
import com.tinsae.socialmediaplatform.common.config.AppMailProperties;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.service.EmailService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AppMailProperties mailProperties;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void forgotPasswordReturnsGenericMessageAndStoresTokenForActiveUser() {
        User user = TestDataFactory.user("tinsae");
        when(userRepository.findByEmail("tinsae@example.com")).thenReturn(Optional.of(user));
        when(mailProperties.getFrontendResetPasswordUrl()).thenReturn("http://localhost:4200/reset-password");
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = passwordResetService.forgotPassword(new ForgotPasswordRequest(" TINSAE@example.com "));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken token = tokenCaptor.getValue();
        assertThat(response.message()).isEqualTo("If an account exists for this email, a reset link has been sent.");
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).hasSize(64);
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
        verify(emailService).sendPasswordResetEmail(
                org.mockito.Mockito.eq("tinsae@example.com"),
                org.mockito.Mockito.startsWith("http://localhost:4200/reset-password?token=")
        );
    }

    @Test
    void forgotPasswordDoesNotRevealMissingEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        var response = passwordResetService.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

        assertThat(response.message()).isEqualTo("If an account exists for this email, a reset link has been sent.");
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPasswordDoesNotCreateTokenForInactiveUser() {
        User user = TestDataFactory.user("tinsae");
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("tinsae@example.com")).thenReturn(Optional.of(user));

        passwordResetService.forgotPassword(new ForgotPasswordRequest("tinsae@example.com"));

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordUpdatesPasswordMarksTokenUsedAndRevokesRefreshTokens() {
        User user = TestDataFactory.user("tinsae");
        PasswordResetToken resetToken = passwordResetToken(user, "reset-token", Instant.now().plusSeconds(300));
        RefreshToken firstRefreshToken = refreshToken(user);
        RefreshToken secondRefreshToken = refreshToken(user);
        when(passwordResetTokenRepository.findByTokenHash(hashToken("reset-token"))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
                .thenReturn(List.of(firstRefreshToken, secondRefreshToken));

        var response = passwordResetService.resetPassword(
                new ResetPasswordRequest("reset-token", "NewPassword123")
        );

        assertThat(response.message()).isEqualTo("Password has been reset successfully. Log in with your new password.");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        assertThat(resetToken.getUsedAt()).isNotNull();
        assertThat(firstRefreshToken.getRevokedAt()).isNotNull();
        assertThat(secondRefreshToken.getRevokedAt()).isNotNull();
        verify(passwordResetTokenRepository).save(resetToken);
        verify(userRepository).save(user);
        verify(refreshTokenRepository).save(firstRefreshToken);
        verify(refreshTokenRepository).save(secondRefreshToken);
    }

    @Test
    void resetPasswordRejectsMissingToken() {
        when(passwordResetTokenRepository.findByTokenHash(hashToken("missing"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("missing", "NewPassword123")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired password reset token.");
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        User user = TestDataFactory.user("tinsae");
        PasswordResetToken resetToken = passwordResetToken(user, "expired-token", Instant.now().minusSeconds(1));
        when(passwordResetTokenRepository.findByTokenHash(hashToken("expired-token"))).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("expired-token", "NewPassword123")
        )).isInstanceOf(BadCredentialsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPasswordRejectsUsedToken() {
        User user = TestDataFactory.user("tinsae");
        PasswordResetToken resetToken = passwordResetToken(user, "used-token", Instant.now().plusSeconds(300));
        resetToken.setUsedAt(Instant.now());
        when(passwordResetTokenRepository.findByTokenHash(hashToken("used-token"))).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("used-token", "NewPassword123")
        )).isInstanceOf(BadCredentialsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPasswordRejectsInactiveUser() {
        User user = TestDataFactory.user("tinsae");
        user.setStatus(UserStatus.SUSPENDED);
        PasswordResetToken resetToken = passwordResetToken(user, "reset-token", Instant.now().plusSeconds(300));
        when(passwordResetTokenRepository.findByTokenHash(hashToken("reset-token"))).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new ResetPasswordRequest("reset-token", "NewPassword123")
        )).isInstanceOf(BadCredentialsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    private PasswordResetToken passwordResetToken(User user, String token, Instant expiresAt) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setId(UUID.randomUUID());
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(token));
        resetToken.setExpiresAt(expiresAt);
        return resetToken;
    }

    private RefreshToken refreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
        return refreshToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
