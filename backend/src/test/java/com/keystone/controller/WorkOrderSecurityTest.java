package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.service.AuthorizationService;
import com.keystone.service.WorkOrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkOrderController.class)
@Import(MethodSecurityTestConfig.class)
class WorkOrderSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkOrderService workOrderService;

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
    void getWorkOrders_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void getWorkOrders_WhenCustomerRole_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(false);

        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void getWorkOrders_WhenTechnician_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrders(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyString()))
                .thenReturn(WorkOrderPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void startWorkOrder_WhenDispatcher_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("START_WORK"))).thenReturn(false);

        mockMvc.perform(post("/api/work-orders/7/start"))
                .andExpect(status().isForbidden());
    }
}
