package com.tinsae.socialmediaplatform.profile.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.profile.dto.UpdateProfileRequest;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.profile.repository.ProfileRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfileReturnsProfileForUser() {
        User user = TestDataFactory.user("profile-user");
        Profile profile = user.getProfile();
        when(userService.findActiveUserById(user.getId())).thenReturn(user);
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        var response = profileService.getProfile(user.getId());

        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.displayName()).isEqualTo("profile-user");
    }

    @Test
    void updateProfileUpdatesOwnProfile() {
        User user = TestDataFactory.user("profile-user");
        Profile profile = user.getProfile();
        var jwt = TestDataFactory.jwt(user);
        var request = new UpdateProfileRequest(
                "Display",
                "Bio",
                "avatar",
                "cover",
                "Bole Road",
                "Addis Ababa",
                "Addis Ababa",
                "Ethiopia",
                "https://example.com"
        );
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(profileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);

        var response = profileService.updateProfile(user.getId(), request, jwt);

        assertThat(response.displayName()).isEqualTo("Display");
        assertThat(response.bio()).isEqualTo("Bio");
        assertThat(response.street()).isEqualTo("Bole Road");
        assertThat(response.city()).isEqualTo("Addis Ababa");
        assertThat(response.state()).isEqualTo("Addis Ababa");
        assertThat(response.country()).isEqualTo("Ethiopia");
    }

    @Test
    void updateProfileRejectsDifferentUser() {
        User owner = TestDataFactory.user("owner");
        User other = TestDataFactory.user("other");
        var jwt = TestDataFactory.jwt(other);
        var request = new UpdateProfileRequest("Display", null, null, null, null, null, null, null, null);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(other);

        assertThatThrownBy(() -> profileService.updateProfile(owner.getId(), request, jwt))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You can only update your own profile.");
    }
}
