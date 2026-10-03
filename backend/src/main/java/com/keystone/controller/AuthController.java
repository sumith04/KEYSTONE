package com.keystone.controller;

import com.keystone.dto.*;
import com.keystone.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.keystone.enums.Permission;
import com.keystone.service.AuthorizationService;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthorizationService authorizationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        MessageResponse response = authService.logout(bearerToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/permissions")
    public ResponseEntity<UserPermissionsResponse> getCurrentUserPermissions(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = authService.getCurrentUserProfile(userDetails.getUsername());
        Set<Permission> permissions = authorizationService.getPermissionsForUser(userDetails.getUsername());
        return ResponseEntity.ok(UserPermissionsResponse.builder()
                .role(user.getRole())
                .permissions(permissions)
                .build());
    }
}
