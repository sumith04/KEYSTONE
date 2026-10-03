package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CustomerPageResponse;
import com.keystone.dto.SitePageResponse;
import com.keystone.service.AuthorizationService;
import com.keystone.service.CustomerService;
import com.keystone.service.SiteService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CustomerController.class, SiteController.class})
@Import(MethodSecurityTestConfig.class)
class CustomerSiteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private SiteService siteService;

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
    void getCustomers_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSites_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/sites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void getCustomers_WhenCustomerRole_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_CUSTOMER"))).thenReturn(false);

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void getSites_WhenCustomerRole_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_SITE"))).thenReturn(false);

        mockMvc.perform(get("/api/sites"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getCustomers_WhenManager_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_CUSTOMER"))).thenReturn(true);
        when(customerService.getCustomers(anyInt(), anyInt(), anyString(), isNull(), isNull()))
                .thenReturn(CustomerPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void getSites_WhenTechnician_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_SITE"))).thenReturn(true);
        when(siteService.getSites(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(SitePageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
