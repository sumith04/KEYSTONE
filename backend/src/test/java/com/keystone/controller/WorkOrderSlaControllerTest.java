package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.WorkOrderSlaPageResponse;
import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.enums.SlaStatus;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.WorkOrderService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

@WebMvcTest(controllers = WorkOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class WorkOrderSlaControllerTest {

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

    @Test
    @WithMockUser(username = "tech.a@keystone.com", roles = {"TECHNICIAN"})
    void getWorkOrderSla_WhenAssigned_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrderSla(7L, "tech.a@keystone.com"))
                .thenReturn(WorkOrderSlaResponse.builder()
                        .workOrderId(7L)
                        .workOrderNumber("WO-000007")
                        .slaStatus(SlaStatus.ON_TRACK)
                        .build());

        mockMvc.perform(get("/api/work-orders/7/sla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slaStatus").value("ON_TRACK"));
    }

    @Test
    @WithMockUser(username = "tech.b@keystone.com", roles = {"TECHNICIAN"})
    void getWorkOrderSla_WhenOtherTechnician_ShouldReturn404() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrderSla(7L, "tech.b@keystone.com"))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 7"));

        mockMvc.perform(get("/api/work-orders/7/sla"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getWorkOrderSlaList_WhenAuthorized_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrderSlaList(
                anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyString()))
                .thenReturn(WorkOrderSlaPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/work-orders/sla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}

@WebMvcTest(controllers = WorkOrderController.class)
@Import(MethodSecurityTestConfig.class)
class WorkOrderSlaSecurityTest {

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
    void getWorkOrderSla_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/work-orders/7/sla"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void getWorkOrderSla_WhenCustomer_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(false);

        mockMvc.perform(get("/api/work-orders/7/sla"))
                .andExpect(status().isForbidden());
    }
}
