package com.keystone.service;

import com.keystone.config.JwtTokenProvider;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.*;
import com.keystone.entity.PasswordResetToken;
import com.keystone.entity.User;
import com.keystone.enums.Role;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.InvalidCredentialsException;
import com.keystone.exception.InvalidTokenException;
import com.keystone.repository.PasswordResetTokenRepository;
import com.keystone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .userEmail("jane.smith@example.com")
                .password("encoded_password")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .userEmail("jane.smith@example.com")
                .password("Password123!")
                .phone("555-0199")
                .role(Role.TECHNICIAN)
                .build();

        loginRequest = LoginRequest.builder()
                .userEmail("jane.smith@example.com")
                .password("Password123!")
                .build();
    }

    @Test
    void register_WithValidData_ShouldCreateUserAndReturnToken() {
        when(userRepository.existsByUserEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt_sample_token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt_sample_token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("jane.smith@example.com", response.getUser().getUserEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded_password", userCaptor.getValue().getPassword());
    }

    @Test
    void register_WithDuplicateEmail_ShouldThrowException() {
        when(userRepository.existsByUserEmail("jane.smith@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        when(userRepository.findByUserEmail("jane.smith@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "encoded_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(sampleUser)).thenReturn("jwt_login_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt_login_token", response.getAccessToken());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        when(userRepository.findByUserEmail("jane.smith@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", "encoded_password")).thenReturn(false);

        loginRequest.setPassword("WrongPassword");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_WithUnknownEmail_ShouldThrowException() {
        when(userRepository.findByUserEmail("unknown@example.com")).thenReturn(Optional.empty());

        loginRequest.setUserEmail("unknown@example.com");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void logout_ShouldRevokeToken() {
        String tokenHeader = "Bearer sample_token_to_revoke";

        MessageResponse response = authService.logout(tokenHeader);

        assertNotNull(response);
        verify(tokenBlacklistService).revokeToken("sample_token_to_revoke");
    }

    @Test
    void forgotPassword_WhenUserExists_ShouldGenerateResetTokenAndSendEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .userEmail("jane.smith@example.com")
                .build();

        when(userRepository.findByUserEmail("jane.smith@example.com")).thenReturn(Optional.of(sampleUser));

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("password reset instructions have been sent"));

        verify(passwordResetTokenRepository).deleteByUser(sampleUser);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("jane.smith@example.com"), anyString());
    }

    @Test
    void resetPassword_WithValidToken_ShouldUpdateUserPassword() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .token("valid_reset_token")
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken("valid_reset_token")
                .newPassword("NewPassword123!")
                .build();

        when(passwordResetTokenRepository.findByToken("valid_reset_token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new_encoded_password");

        MessageResponse response = authService.resetPassword(request);

        assertNotNull(response);
        assertEquals("Password has been reset successfully", response.getMessage());
        assertEquals("new_encoded_password", sampleUser.getPassword());
        assertTrue(token.isUsed());
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .token("expired_reset_token")
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken("expired_reset_token")
                .newPassword("NewPassword123!")
                .build();

        when(passwordResetTokenRepository.findByToken("expired_reset_token")).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> authService.resetPassword(request));
    }
}
