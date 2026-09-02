package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.role.entity.UserRole;
import com.tinsae.socialmediaplatform.user.dto.UserResponse;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                roles,
                user.getCreatedAt()
        );
    }

    public static UserSummaryResponse toSummary(User user) {
        Profile profile = user.getProfile();

        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                profile != null ? profile.getDisplayName() : null,
                profile != null ? profile.getAvatarUrl() : null
        );
    }
}
