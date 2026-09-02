package com.tinsae.socialmediaplatform.user.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUserReturnsAuthenticatedUserResponse() {
        User user = TestDataFactory.user("user");
        TestDataFactory.userRole(user, TestDataFactory.role("USER"));
        var jwt = TestDataFactory.jwt(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = userService.getCurrentUser(jwt);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.username()).isEqualTo("user");
        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    void getUserByIdReturnsUserResponse() {
        User user = TestDataFactory.user("user");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = userService.getUserById(user.getId());

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void getAuthenticatedUserReturnsUser() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        User response = userService.getAuthenticatedUser(jwt);

        assertThat(response).isSameAs(user);
    }

    @Test
    void getAuthenticatedUserRejectsSuspendedUser() {
        User user = TestDataFactory.user("user");
        user.setStatus(UserStatus.SUSPENDED);
        var jwt = TestDataFactory.jwt(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getAuthenticatedUser(jwt))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("Account is not active.");
    }

    @Test
    void findUserByIdReturnsUser() {
        User user = TestDataFactory.user("user");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        User response = userService.findUserById(user.getId());

        assertThat(response).isSameAs(user);
    }

    @Test
    void findUserByIdRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found.");
    }

    @Test
    void findActiveUserByIdReturnsActiveUser() {
        User user = TestDataFactory.user("user");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        User response = userService.findActiveUserById(user.getId());

        assertThat(response).isSameAs(user);
    }

    @Test
    void findActiveUserByIdRejectsSuspendedUser() {
        User user = TestDataFactory.user("user");
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.findActiveUserById(user.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a"})
    void searchUsersReturnsEmptyListForShortQueries(String query) {
        assertThat(userService.searchUsers(query, 10)).isEmpty();
    }

    @Test
    void searchUsersReturnsEmptyListForNullQuery() {
        assertThat(userService.searchUsers(null, 10)).isEmpty();
    }

    @Test
    void searchUsersTrimsQueryAndClampsLimitBelowOne() {
        User user = TestDataFactory.user("user");
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.searchUsers(eq("user"), eq(UserStatus.ACTIVE), pageableCaptor.capture()))
                .thenReturn(List.of(user));

        var response = userService.searchUsers("  user  ", 0);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(user.getId());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void searchUsersClampsLimitAboveTwenty() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.searchUsers(eq("user"), eq(UserStatus.ACTIVE), pageableCaptor.capture()))
                .thenReturn(List.of());

        assertThat(userService.searchUsers("user", 100)).isEmpty();

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }
}
