package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.DashboardSummaryResponse;
import com.keystone.dto.RecentWorkOrderResponse;
import com.keystone.dto.WorkOrderTrendResponse;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

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
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getSummary_WhenAuthorized_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_DASHBOARD"))).thenReturn(true);
        when(dashboardService.getSummary(eq("2026-01-01"), eq("2026-01-31"), eq("manager@keystone.com")))
                .thenReturn(DashboardSummaryResponse.builder().totalWorkOrders(120).openWorkOrders(63).build());

        mockMvc.perform(get("/api/dashboard/summary").param("from", "2026-01-01").param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkOrders").value(120))
                .andExpect(jsonPath("$.openWorkOrders").value(63));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getSummary_WhenInvalidRange_ShouldReturn400() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_DASHBOARD"))).thenReturn(true);
        when(dashboardService.getSummary(eq("2026-02-01"), eq("2026-01-01"), eq("manager@keystone.com")))
                .thenThrow(new ApiException("'from' must be less than or equal to 'to'.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/api/dashboard/summary").param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getRecentWorkOrders_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_DASHBOARD"))).thenReturn(true);
        when(dashboardService.getRecentWorkOrders(isNull(), isNull(), eq(10), eq("manager@keystone.com")))
                .thenReturn(List.of(RecentWorkOrderResponse.builder()
                        .id(7L)
                        .workOrderNumber("WO-000007")
                        .status(WorkOrderStatus.NEW)
                        .build()));

        mockMvc.perform(get("/api/dashboard/recent-work-orders").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workOrderNumber").value("WO-000007"));
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void getWorkOrderTrend_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_DASHBOARD"))).thenReturn(true);
        when(dashboardService.getWorkOrderTrend(isNull(), isNull(), eq("DAY"), eq("dispatcher@keystone.com")))
                .thenReturn(List.of(WorkOrderTrendResponse.builder().period("2026-01-01").created(4).build()));

        mockMvc.perform(get("/api/dashboard/work-order-trend").param("interval", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].created").value(4));
    }
}
