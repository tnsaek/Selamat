package com.tinsae.socialmediaplatform.auth.service;

import com.tinsae.socialmediaplatform.auth.dto.ForgotPasswordRequest;
import com.tinsae.socialmediaplatform.auth.dto.PasswordResetMessageResponse;
import com.tinsae.socialmediaplatform.auth.dto.ResetPasswordRequest;
import com.tinsae.socialmediaplatform.auth.entity.PasswordResetToken;
import com.tinsae.socialmediaplatform.auth.entity.RefreshToken;
import com.tinsae.socialmediaplatform.auth.repository.PasswordResetTokenRepository;
import com.tinsae.socialmediaplatform.auth.repository.RefreshTokenRepository;
import com.tinsae.socialmediaplatform.common.config.AppMailProperties;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.service.EmailService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "If an account exists for this email, a reset link has been sent.";
    private static final String PASSWORD_RESET_SUCCESS_MESSAGE =
            "Password has been reset successfully. Log in with your new password.";
    private static final int TOKEN_BYTES = 32;
    private static final long TOKEN_EXPIRY_SECONDS = 30 * 60L;

    private final AppMailProperties mailProperties;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;

    public PasswordResetService(
            AppMailProperties mailProperties,
            EmailService emailService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.mailProperties = mailProperties;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PasswordResetMessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();

        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::createAndLogResetToken);

        return new PasswordResetMessageResponse(GENERIC_FORGOT_PASSWORD_MESSAGE);
    }

    @Transactional
    public PasswordResetMessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired password reset token."));

        if (resetToken.isUsed() || resetToken.isExpired() || resetToken.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Invalid or expired password reset token.");
        }

        Instant now = Instant.now();
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsedAt(now);

        revokeRefreshTokens(user, now);
        passwordResetTokenRepository.save(resetToken);
        userRepository.save(user);

        return new PasswordResetMessageResponse(PASSWORD_RESET_SUCCESS_MESSAGE);
    }

    private void createAndLogResetToken(User user) {
        String token = generateToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(token));
        resetToken.setExpiresAt(Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS));
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink(token));
    }

    private void revokeRefreshTokens(User user, Instant revokedAt) {
        for (RefreshToken refreshToken : refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            refreshToken.setRevokedAt(revokedAt);
            refreshTokenRepository.save(refreshToken);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resetLink(String token) {
        String baseUrl = mailProperties.getFrontendResetPasswordUrl();
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessRuleException("Unable to hash password reset token.");
        }
    }
}
