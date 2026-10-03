package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.ServiceRequestPageResponse;
import com.keystone.dto.ServiceRequestResponse;
import com.keystone.service.AuthorizationService;
import com.keystone.service.ServiceRequestService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ServiceRequestController.class)
@Import(MethodSecurityTestConfig.class)
class ServiceRequestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceRequestService serviceRequestService;
    @MockBean(name = "authorizationService")
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

    @BeforeEach
    void allowFilterChainToContinue() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void list_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/service-requests")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "acme@keystone.com", roles = {"CUSTOMER"})
    void list_WhenCustomer_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_SERVICE_REQUEST"))).thenReturn(false);
        mockMvc.perform(get("/api/service-requests")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tech@keystone.com", roles = {"TECHNICIAN"})
    void list_WhenTechnician_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_SERVICE_REQUEST"))).thenReturn(false);
        mockMvc.perform(get("/api/service-requests")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void list_WhenManager_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_SERVICE_REQUEST"))).thenReturn(true);
        when(serviceRequestService.getRequests(0, 10, "createdAt", null, null, null, null))
                .thenReturn(ServiceRequestPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());
        mockMvc.perform(get("/api/service-requests")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void convert_WhenDispatcherAuthorized_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("CONVERT_SERVICE_REQUEST"))).thenReturn(true);
        when(serviceRequestService.convertToWorkOrder(50L, "dispatcher@keystone.com"))
                .thenReturn(ServiceRequestResponse.builder().id(50L).build());
        mockMvc.perform(post("/api/service-requests/50/convert-to-work-order").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "acme@keystone.com", roles = {"CUSTOMER"})
    void convert_WhenCustomer_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("CONVERT_SERVICE_REQUEST"))).thenReturn(false);
        mockMvc.perform(post("/api/service-requests/50/convert-to-work-order").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
