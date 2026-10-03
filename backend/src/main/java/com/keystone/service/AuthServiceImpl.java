package com.keystone.service;

import com.keystone.config.JwtTokenProvider;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.*;
import com.keystone.entity.PasswordResetToken;
import com.keystone.entity.User;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.InvalidCredentialsException;
import com.keystone.exception.InvalidTokenException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.PasswordResetTokenRepository;
import com.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getUserEmail().trim().toLowerCase();

        if (userRepository.existsByUserEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email address " + normalizedEmail + " already exists.");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .userEmail(normalizedEmail)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .message("User registered successfully")
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getUserEmail().trim().toLowerCase();

        User user = userRepository.findByUserEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled. Please contact system administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .message("Login successful")
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Override
    public MessageResponse logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String rawToken = bearerToken.substring(7);
            tokenBlacklistService.revokeToken(rawToken);
        } else if (bearerToken != null && !bearerToken.isBlank()) {
            tokenBlacklistService.revokeToken(bearerToken);
        }
        return new MessageResponse("Logged out successfully");
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getUserEmail().trim().toLowerCase();
        Optional<User> userOptional = userRepository.findByUserEmail(normalizedEmail);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            passwordResetTokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(24))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(normalizedEmail, token);
        }

        return new MessageResponse("If the account exists, password reset instructions have been sent.");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getResetToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Invalid or expired password reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return new MessageResponse("Password has been reset successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String userEmail) {
        String normalizedEmail = userEmail.trim().toLowerCase();
        User user = userRepository.findByUserEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
        return UserResponse.fromEntity(user);
    }
}
