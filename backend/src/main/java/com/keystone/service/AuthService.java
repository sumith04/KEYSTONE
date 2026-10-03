package com.keystone.service;

import com.keystone.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    MessageResponse logout(String bearerToken);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    UserResponse getCurrentUserProfile(String userEmail);
}
