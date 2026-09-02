package com.tinsae.socialmediaplatform.auth.repository;

import com.tinsae.socialmediaplatform.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
}
