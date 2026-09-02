package com.tinsae.socialmediaplatform.auth.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.auth.dto.AuthResponse;
import com.tinsae.socialmediaplatform.auth.dto.ForgotPasswordRequest;
import com.tinsae.socialmediaplatform.auth.dto.LoginRequest;
import com.tinsae.socialmediaplatform.auth.dto.PasswordResetMessageResponse;
import com.tinsae.socialmediaplatform.auth.dto.RefreshTokenRequest;
import com.tinsae.socialmediaplatform.auth.dto.ResetPasswordRequest;
import com.tinsae.socialmediaplatform.auth.dto.SignUpRequest;
import com.tinsae.socialmediaplatform.auth.service.AuthService;
import com.tinsae.socialmediaplatform.auth.service.PasswordResetService;
import com.tinsae.socialmediaplatform.common.mapper.UserMapper;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    @Test
    void signUpReturnsCreatedAuthResponse() {
        SignUpRequest request = new SignUpRequest("tinsae", "tinsae@example.com", "StrongPassword123");
        AuthResponse authResponse = authResponse();
        when(authService.signUp(request)).thenReturn(authResponse);

        var response = authController.signUp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(authResponse);
        verify(authService).signUp(request);
    }

    @Test
    void loginReturnsOkAuthResponse() {
        LoginRequest request = new LoginRequest("tinsae", "StrongPassword123");
        AuthResponse authResponse = authResponse();
        when(authService.login(request)).thenReturn(authResponse);

        var response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(authResponse);
        verify(authService).login(request);
    }

    @Test
    void refreshReturnsOkAuthResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh");
        AuthResponse authResponse = authResponse();
        when(authService.refresh(request)).thenReturn(authResponse);

        var response = authController.refresh(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(authResponse);
        verify(authService).refresh(request);
    }

    @Test
    void logoutReturnsNoContent() {
        User user = TestDataFactory.user("tinsae");
        var jwt = TestDataFactory.jwt(user);

        var response = authController.logout(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(authService).logout(jwt);
    }

    @Test
    void forgotPasswordReturnsOkMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("tinsae@example.com");
        PasswordResetMessageResponse resetResponse = new PasswordResetMessageResponse("If an account exists.");
        when(passwordResetService.forgotPassword(request)).thenReturn(resetResponse);

        var response = authController.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(resetResponse);
        verify(passwordResetService).forgotPassword(request);
    }

    @Test
    void resetPasswordReturnsOkMessage() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "StrongPassword123");
        PasswordResetMessageResponse resetResponse = new PasswordResetMessageResponse("Password reset.");
        when(passwordResetService.resetPassword(request)).thenReturn(resetResponse);

        var response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(resetResponse);
        verify(passwordResetService).resetPassword(request);
    }

    private AuthResponse authResponse() {
        User user = TestDataFactory.user("tinsae");
        return new AuthResponse("access", "refresh", "Bearer", UserMapper.toResponse(user));
    }
}
