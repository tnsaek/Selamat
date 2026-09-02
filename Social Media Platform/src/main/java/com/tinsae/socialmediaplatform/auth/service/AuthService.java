package com.tinsae.socialmediaplatform.auth.service;

import com.tinsae.socialmediaplatform.auth.dto.AuthResponse;
import com.tinsae.socialmediaplatform.auth.dto.LoginRequest;
import com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest;
import com.tinsae.socialmediaplatform.auth.dto.SignUpRequest;
import com.tinsae.socialmediaplatform.auth.entity.RefreshToken;
import com.tinsae.socialmediaplatform.auth.repository.RefreshTokenRepository;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.DuplicateResourceException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.common.mapper.UserMapper;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.profile.repository.ProfileRepository;
import com.tinsae.socialmediaplatform.role.entity.Role;
import com.tinsae.socialmediaplatform.role.entity.UserRole;
import com.tinsae.socialmediaplatform.role.entity.UserRoleId;
import com.tinsae.socialmediaplatform.role.repository.RoleRepository;
import com.tinsae.socialmediaplatform.role.repository.UserRoleRepository;
import com.tinsae.socialmediaplatform.security.jwt.JwtTokenService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE_NAME = "USER";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            RefreshTokenRepository refreshTokenRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtDecoder jwtDecoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        createProfile(savedUser);
        assignDefaultRole(savedUser);

        return authResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByUsernameOrEmail(identifier, identifier.toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid username, email, or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username, email, or password.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedActionException("Account is not active.");
        }

        return authResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Jwt jwt = decodeRefreshToken(request.refreshToken());
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedActionException("Account is not active.");
        }

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(hashToken(request.refreshToken()))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));

        if (!refreshToken.getUser().getId().equals(user.getId()) || refreshToken.isExpired()) {
            refreshToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(refreshToken);
            throw new BadCredentialsException("Invalid refresh token.");
        }

        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        return authResponse(user);
    }

    @Transactional
    public void logout(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Instant revokedAt = Instant.now();
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(refreshToken -> {
                    refreshToken.setRevokedAt(revokedAt);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    private void createProfile(User user) {
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setDisplayName(user.getUsername());
        profileRepository.save(profile);
        user.setProfile(profile);
    }

    private void assignDefaultRole(User user) {
        Role role = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseGet(this::createDefaultUserRole);

        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getId(), role.getId()));
        userRole.setUser(user);
        userRole.setRole(role);

        userRoleRepository.save(userRole);
        user.getRoles().add(userRole);
    }

    private Role createDefaultUserRole() {
        Role role = new Role();
        role.setName(DEFAULT_ROLE_NAME);
        role.setDescription("Default role for registered users.");
        return roleRepository.save(role);
    }

    private AuthResponse authResponse(User user) {
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        saveRefreshToken(user, refreshToken);

        return new AuthResponse(
                jwtTokenService.generateAccessToken(user),
                refreshToken,
                TOKEN_TYPE,
                UserMapper.toResponse(user)
        );
    }

    private void saveRefreshToken(User user, String token) {
        Jwt jwt = jwtDecoder.decode(token);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(token));
        refreshToken.setExpiresAt(jwt.getExpiresAt());
        refreshTokenRepository.save(refreshToken);
    }

    private Jwt decodeRefreshToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
                throw new BadCredentialsException("Invalid refresh token.");
            }
            return jwt;
        } catch (JwtException exception) {
            throw new BadCredentialsException("Invalid refresh token.", exception);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessRuleException("Unable to hash refresh token.");
        }
    }
}
