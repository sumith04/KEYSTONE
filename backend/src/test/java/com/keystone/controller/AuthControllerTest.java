package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.*;
import com.keystone.enums.Role;
import com.keystone.service.AuthService;
import com.keystone.config.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Manager")
                .userEmail("alice@keystone.com")
                .password("SecurePass123!")
                .role(Role.MANAGER)
                .build();

        loginRequest = LoginRequest.builder()
                .userEmail("alice@keystone.com")
                .password("SecurePass123!")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Manager")
                .userEmail("alice@keystone.com")
                .role(Role.MANAGER)
                .enabled(true)
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("mock_jwt_token")
                .tokenType("Bearer")
                .message("Success")
                .user(userResponse)
                .build();
    }

    @Test
    void register_ShouldReturn201Created() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock_jwt_token"))
                .andExpect(jsonPath("$.user.userEmail").value("alice@keystone.com"));
    }

    @Test
    void login_ShouldReturn200Ok() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock_jwt_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void logout_ShouldReturn200Ok() throws Exception {
        when(authService.logout(any())).thenReturn(new MessageResponse("Logged out successfully"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer mock_jwt_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @WithMockUser(username = "alice@keystone.com")
    void getCurrentUser_ShouldReturn200Ok_WhenAuthenticated() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Manager")
                .userEmail("alice@keystone.com")
                .role(Role.MANAGER)
                .enabled(true)
                .build();

        when(authService.getCurrentUserProfile("alice@keystone.com")).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("alice@keystone.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }
}
