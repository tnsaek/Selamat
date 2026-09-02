package com.tinsae.socialmediaplatform.auth.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.auth.dto.LoginRequest;
import com.tinsae.socialmediaplatform.auth.dto.SignUpRequest;
import com.tinsae.socialmediaplatform.auth.entity.RefreshToken;
import com.tinsae.socialmediaplatform.auth.repository.RefreshTokenRepository;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.exception.DuplicateResourceException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.profile.repository.ProfileRepository;
import com.tinsae.socialmediaplatform.role.entity.Role;
import com.tinsae.socialmediaplatform.role.entity.UserRole;
import com.tinsae.socialmediaplatform.role.repository.RoleRepository;
import com.tinsae.socialmediaplatform.role.repository.UserRoleRepository;
import com.tinsae.socialmediaplatform.security.jwt.JwtTokenService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void signUpCreatesUserProfileRoleAndTokens() {
        Role role = TestDataFactory.role("USER");
        when(userRepository.existsByUsername("tinsae")).thenReturn(false);
        when(userRepository.existsByEmail("tinsae@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile profile = invocation.getArgument(0);
            profile.setId(UUID.randomUUID());
            return profile;
        });
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh");
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.signUp(new SignUpRequest("tinsae", "tinsae@example.com", "StrongPassword123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().username()).isEqualTo("tinsae");
        assertThat(response.user().roles()).containsExactly("USER");
        verify(profileRepository).save(any(Profile.class));
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void signUpCreatesDefaultRoleWhenRoleDoesNotExist() {
        when(userRepository.existsByUsername("tinsae")).thenReturn(false);
        when(userRepository.existsByEmail("tinsae@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile profile = invocation.getArgument(0);
            profile.setId(UUID.randomUUID());
            return profile;
        });
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(UUID.randomUUID());
            return role;
        });
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtTokenService.generateRefreshToken(any(User.class))).thenReturn("refresh");
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.signUp(new SignUpRequest("tinsae", "tinsae@example.com", "StrongPassword123"));

        assertThat(response.user().roles()).containsExactly("USER");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void signUpRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("tinsae")).thenReturn(true);

        assertThatThrownBy(() -> authService.signUp(new SignUpRequest("tinsae", "tinsae@example.com", "password123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username is already taken.");
    }

    @Test
    void signUpRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("tinsae")).thenReturn(false);
        when(userRepository.existsByEmail("tinsae@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signUp(new SignUpRequest("tinsae", "tinsae@example.com", "password123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email is already registered.");
    }

    @Test
    void loginReturnsTokensForActiveUser() {
        User user = TestDataFactory.user("tinsae");
        TestDataFactory.userRole(user, TestDataFactory.role("USER"));
        when(userRepository.findByUsernameOrEmail("tinsae", "tinsae")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.generateAccessToken(user)).thenReturn("access");
        when(jwtTokenService.generateRefreshToken(user)).thenReturn("refresh");
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.login(new LoginRequest("tinsae", "correct"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("tinsae");
    }

    @Test
    void loginRejectsUnknownUser() {
        when(userRepository.findByUsernameOrEmail("missing", "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "password")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username, email, or password.");
    }

    @Test
    void loginRejectsBadPassword() {
        User user = TestDataFactory.user("tinsae");
        when(userRepository.findByUsernameOrEmail("tinsae", "tinsae")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("tinsae", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsInactiveUser() {
        User user = TestDataFactory.user("tinsae");
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByUsernameOrEmail("tinsae", "tinsae")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("tinsae", "correct")))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("Account is not active.");
    }

    @Test
    void refreshRotatesValidRefreshTokenAndReturnsNewTokens() {
        User user = TestDataFactory.user("tinsae");
        TestDataFactory.userRole(user, TestDataFactory.role("USER"));
        RefreshToken storedRefreshToken = refreshToken(user, "old-refresh", Instant.now().plusSeconds(3600));
        when(jwtDecoder.decode("old-refresh")).thenReturn(refreshJwt(user.getId(), "old-refresh", "refresh"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken("old-refresh")))
                .thenReturn(Optional.of(storedRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtTokenService.generateRefreshToken(user)).thenReturn("new-refresh");
        when(jwtDecoder.decode("new-refresh")).thenReturn(refreshJwt(user.getId(), "new-refresh", "refresh"));

        var response = authService.refresh(new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(storedRefreshToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedRefreshToken);
    }

    @Test
    void refreshRejectsAccessToken() {
        User user = TestDataFactory.user("tinsae");
        when(jwtDecoder.decode("access-token")).thenReturn(refreshJwt(user.getId(), "access-token", "access"));

        assertThatThrownBy(() -> authService.refresh(
                new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("access-token")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid refresh token.");
    }

    @Test
    void refreshRejectsJwtDecodeFailure() {
        when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("bad"));

        assertThatThrownBy(() -> authService.refresh(
                new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("bad-token")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid refresh token.");
    }

    @Test
    void refreshRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt(userId, "refresh", "refresh"));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(
                new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("refresh")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid refresh token.");
    }

    @Test
    void refreshRejectsInactiveUser() {
        User user = TestDataFactory.user("tinsae");
        user.setStatus(UserStatus.SUSPENDED);
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt(user.getId(), "refresh", "refresh"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(
                new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("refresh")
        )).isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("Account is not active.");
    }

    @Test
    void refreshRejectsMissingStoredToken() {
        User user = TestDataFactory.user("tinsae");
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt(user.getId(), "refresh", "refresh"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken("refresh")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(
                new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("refresh")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid refresh token.");
    }

    @Test
    void refreshRevokesAndRejectsMismatchedStoredTokenUser() {
        User user = TestDataFactory.user("tinsae");
        RefreshToken storedRefreshToken = refreshToken(TestDataFactory.user("other"), "refresh", Instant.now().plusSeconds(3600));
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt(user.getId(), "refresh", "refresh"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken("refresh")))
                .thenReturn(Optional.of(storedRefreshToken));

        assertThatThrownBy(() -> authService.refresh(
                new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("refresh")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid refresh token.");
        assertThat(storedRefreshToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedRefreshToken);
    }

    @Test
    void refreshRevokesAndRejectsExpiredStoredToken() {
        User user = TestDataFactory.user("tinsae");
        RefreshToken storedRefreshToken = refreshToken(user, "refresh", Instant.now().minusSeconds(60));
        when(jwtDecoder.decode("refresh")).thenReturn(refreshJwt(user.getId(), "refresh", "refresh"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken("refresh")))
                .thenReturn(Optional.of(storedRefreshToken));

        assertThatThrownBy(() -> authService.refresh(
                new com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest("refresh")
        )).isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid refresh token.");
        assertThat(storedRefreshToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedRefreshToken);
    }

    @Test
    void logoutRevokesAllActiveRefreshTokensForUser() {
        User user = TestDataFactory.user("tinsae");
        RefreshToken first = refreshToken(user, "first", Instant.now().plusSeconds(3600));
        RefreshToken second = refreshToken(user, "second", Instant.now().plusSeconds(3600));
        var jwt = TestDataFactory.jwt(user);
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
                .thenReturn(List.of(first, second));

        authService.logout(jwt);

        assertThat(first.getRevokedAt()).isNotNull();
        assertThat(second.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(first);
        verify(refreshTokenRepository).save(second);
    }

    private Jwt refreshJwt() {
        Instant now = Instant.now();
        return new Jwt(
                "refresh",
                now,
                now.plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", UUID.randomUUID().toString(), "token_type", "refresh")
        );
    }

    private Jwt refreshJwt(UUID userId, String tokenValue, String tokenType) {
        Instant now = Instant.now();
        return new Jwt(
                tokenValue,
                now,
                now.plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", userId.toString(), "token_type", tokenType)
        );
    }

    private RefreshToken refreshToken(User user, String token, Instant expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(token));
        refreshToken.setExpiresAt(expiresAt);
        return refreshToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
