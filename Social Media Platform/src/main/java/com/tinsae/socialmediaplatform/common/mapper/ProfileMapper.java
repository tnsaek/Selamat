package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.profile.dto.ProfileResponse;
import com.tinsae.socialmediaplatform.profile.entity.Profile;

public final class ProfileMapper {

    private ProfileMapper() {
    }

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getCoverImageUrl(),
                profile.getStreet(),
                profile.getCity(),
                profile.getState(),
                profile.getCountry(),
                profile.getWebsiteUrl()
        );
    }
}
