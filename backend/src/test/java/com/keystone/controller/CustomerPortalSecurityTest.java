package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.service.AuthorizationService;
import com.keystone.service.CustomerPortalService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerPortalController.class)
@Import(MethodSecurityTestConfig.class)
class CustomerPortalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerPortalService customerPortalService;
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
    void getProfile_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/customer/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "tech@keystone.com", roles = {"TECHNICIAN"})
    void getProfile_WhenTechnician_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/customer/profile")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "acme@keystone.com", roles = {"CUSTOMER"})
    void getRequests_WhenCustomerHasPermission_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_OWN_REQUEST"))).thenReturn(true);
        when(serviceRequestService.getMyRequests(0, 10, "createdAt", null, null, null, "acme@keystone.com"))
                .thenReturn(com.keystone.dto.ServiceRequestPageResponse.builder()
                        .content(java.util.List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/customer/requests")).andExpect(status().isOk());
    }
}
