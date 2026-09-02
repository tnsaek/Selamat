package com.tinsae.socialmediaplatform.user.service;

import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.common.mapper.UserMapper;
import com.tinsae.socialmediaplatform.user.dto.UserResponse;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Jwt jwt) {
        return UserMapper.toResponse(getAuthenticatedUser(jwt));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        return UserMapper.toResponse(findActiveUserById(userId));
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> searchUsers(String query, int limit) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        int normalizedLimit = Math.min(Math.max(limit, 1), 20);

        return userRepository.searchUsers(normalizedQuery, UserStatus.ACTIVE, PageRequest.of(0, normalizedLimit))
                .stream()
                .map(UserMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUser(Jwt jwt) {
        User user = findUserById(UUID.fromString(jwt.getSubject()));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedActionException("Account is not active.");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Transactional(readOnly = true)
    public User findActiveUserById(UUID userId) {
        User user = findUserById(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResourceNotFoundException("User not found.");
        }

        return user;
    }
}
