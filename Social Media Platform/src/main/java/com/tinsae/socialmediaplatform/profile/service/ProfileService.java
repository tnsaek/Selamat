package com.tinsae.socialmediaplatform.profile.service;

import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.common.mapper.ProfileMapper;
import com.tinsae.socialmediaplatform.profile.dto.ProfileResponse;
import com.tinsae.socialmediaplatform.profile.dto.UpdateProfileRequest;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.profile.repository.ProfileRepository;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserService userService;

    public ProfileService(ProfileRepository profileRepository, UserService userService) {
        this.profileRepository = profileRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        userService.findActiveUserById(userId);
        return ProfileMapper.toResponse(findProfileByUserId(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request, Jwt jwt) {
        if (!userService.getAuthenticatedUser(jwt).getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only update your own profile.");
        }

        Profile profile = findProfileByUserId(userId);
        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setCoverImageUrl(request.coverImageUrl());
        profile.setStreet(request.street());
        profile.setCity(request.city());
        profile.setState(request.state());
        profile.setCountry(request.country());
        profile.setWebsiteUrl(request.websiteUrl());

        return ProfileMapper.toResponse(profileRepository.save(profile));
    }

    private Profile findProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
    }
}
