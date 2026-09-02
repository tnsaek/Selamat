package com.tinsae.socialmediaplatform.user.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.user.dto.UserResponse;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getCurrentUserDelegatesToUserService() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        UserResponse userResponse = userResponse(user.getId());
        when(userService.getCurrentUser(jwt)).thenReturn(userResponse);

        UserResponse response = userController.getCurrentUser(jwt);

        assertThat(response).isSameAs(userResponse);
        verify(userService).getCurrentUser(jwt);
    }

    @Test
    void getUserByIdDelegatesToUserService() {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = userResponse(userId);
        when(userService.getUserById(userId)).thenReturn(userResponse);

        UserResponse response = userController.getUserById(userId);

        assertThat(response).isSameAs(userResponse);
        verify(userService).getUserById(userId);
    }

    @Test
    void searchUsersDelegatesToUserService() {
        UserSummaryResponse summary = new UserSummaryResponse(UUID.randomUUID(), "user", "User", null);
        when(userService.searchUsers("use", 5)).thenReturn(List.of(summary));

        List<UserSummaryResponse> response = userController.searchUsers("use", 5);

        assertThat(response).containsExactly(summary);
        verify(userService).searchUsers("use", 5);
    }

    private UserResponse userResponse(UUID userId) {
        return new UserResponse(
                userId,
                "user",
                "user@example.com",
                UserStatus.ACTIVE,
                Set.of("USER"),
                Instant.now()
        );
    }
}
