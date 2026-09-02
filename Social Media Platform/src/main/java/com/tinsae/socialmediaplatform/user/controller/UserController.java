package com.tinsae.socialmediaplatform.user.controller;

import com.tinsae.socialmediaplatform.user.dto.UserResponse;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(jwt);
    }

    @GetMapping("/search")
    public List<UserSummaryResponse> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return userService.searchUsers(query, limit);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId);
    }
}
