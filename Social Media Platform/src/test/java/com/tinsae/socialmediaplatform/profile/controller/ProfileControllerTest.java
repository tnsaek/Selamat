package com.tinsae.socialmediaplatform.profile.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.profile.dto.ProfileResponse;
import com.tinsae.socialmediaplatform.profile.dto.UpdateProfileRequest;
import com.tinsae.socialmediaplatform.profile.service.ProfileService;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void getProfileDelegatesToProfileService() {
        UUID userId = UUID.randomUUID();
        ProfileResponse profile = profileResponse(userId);
        when(profileService.getProfile(userId)).thenReturn(profile);

        ProfileResponse response = profileController.getProfile(userId);

        assertThat(response).isSameAs(profile);
        verify(profileService).getProfile(userId);
    }

    @Test
    void updateProfileDelegatesToProfileService() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Display",
                "Bio",
                "avatar.png",
                "cover.png",
                "Bole Road",
                "Addis Ababa",
                "Addis Ababa",
                "Ethiopia",
                "https://example.com"
        );
        ProfileResponse profile = profileResponse(user.getId());
        when(profileService.updateProfile(user.getId(), request, jwt)).thenReturn(profile);

        ProfileResponse response = profileController.updateProfile(user.getId(), request, jwt);

        assertThat(response).isSameAs(profile);
        verify(profileService).updateProfile(user.getId(), request, jwt);
    }

    private ProfileResponse profileResponse(UUID userId) {
        return new ProfileResponse(
                UUID.randomUUID(),
                userId,
                "Display",
                "Bio",
                "avatar.png",
                "cover.png",
                "Bole Road",
                "Addis Ababa",
                "Addis Ababa",
                "Ethiopia",
                "https://example.com"
        );
    }
}
