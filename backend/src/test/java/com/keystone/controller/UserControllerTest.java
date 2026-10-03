package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateUserRequest;
import com.keystone.dto.UpdateUserRequest;
import com.keystone.dto.UserPageResponse;
import com.keystone.dto.UserResponse;
import com.keystone.enums.Permission;
import com.keystone.enums.Role;
import com.keystone.service.AuthorizationService;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthorizationService authorizationService;

    @MockBean
    private RolePermissionMapper rolePermissionMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userResponse = UserResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .userEmail("john.doe@keystone.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getUsers_WhenAdmin_ShouldReturn200Ok() throws Exception {
        UserPageResponse pageResponse = UserPageResponse.builder()
                .content(List.of(userResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(authorizationService.hasPermission(any(), eq("VIEW_USER"))).thenReturn(true);
        when(userService.getUsers(0, 10, "createdAt", null, null, null)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userEmail").value("john.doe@keystone.com"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createUser_WhenAdmin_ShouldReturn201Created() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .firstName("New")
                .lastName("User")
                .userEmail("new@keystone.com")
                .password("Password123!")
                .role(Role.TECHNICIAN)
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_USER"))).thenReturn(true);
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void deleteUser_WhenManager_ShouldReturn403Forbidden() throws Exception {
        // MANAGER does not have DELETE_USER permission
        when(authorizationService.hasPermission(any(), eq("DELETE_USER"))).thenReturn(false);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void getUsers_WhenCustomer_ShouldReturn403Forbidden() throws Exception {
        // CUSTOMER does not have VIEW_USER permission
        when(authorizationService.hasPermission(any(), eq("VIEW_USER"))).thenReturn(false);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }
}
