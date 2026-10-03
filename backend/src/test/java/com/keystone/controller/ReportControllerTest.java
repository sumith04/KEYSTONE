package com.keystone.controller;

import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.InventoryReportResponse;
import com.keystone.dto.SlaReportResponse;
import com.keystone.dto.TechnicianPerformanceResponse;
import com.keystone.dto.TimeReportResponse;
import com.keystone.dto.WorkOrderReportResponse;
import com.keystone.service.AuthorizationService;
import com.keystone.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

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
    void getWorkOrderReport_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_REPORT"))).thenReturn(true);
        when(reportService.getWorkOrderReport(isNull(), isNull(), eq("manager@keystone.com")))
                .thenReturn(WorkOrderReportResponse.builder().total(10).created(10).completed(4).build());

        mockMvc.perform(get("/api/reports/work-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getSlaReport_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_REPORT"))).thenReturn(true);
        when(reportService.getSlaReport(eq("2026-01-01"), eq("2026-01-31"), eq("admin@keystone.com")))
                .thenReturn(SlaReportResponse.builder().totalWorkOrdersWithSla(8).breached(2).build());

        mockMvc.perform(get("/api/reports/sla").param("from", "2026-01-01").param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breached").value(2));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getTechnicianPerformance_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_REPORT"))).thenReturn(true);
        when(reportService.getTechnicianPerformance(isNull(), isNull(), eq("manager@keystone.com")))
                .thenReturn(List.of(TechnicianPerformanceResponse.builder().technicianId(25L).assignedCount(4).build()));

        mockMvc.perform(get("/api/reports/technician-performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignedCount").value(4));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getInventoryReport_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_REPORT"))).thenReturn(true);
        when(reportService.getInventoryReport("manager@keystone.com"))
                .thenReturn(InventoryReportResponse.builder().totalParts(80).lowStock(5).totalInventoryValue(new BigDecimal("10.00")).build());

        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStock").value(5));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getTimeReport_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_REPORT"))).thenReturn(true);
        when(reportService.getTimeReport(isNull(), isNull(), eq("manager@keystone.com")))
                .thenReturn(TimeReportResponse.builder().totalLoggedMinutes(90).logsCount(3).build());

        mockMvc.perform(get("/api/reports/time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoggedMinutes").value(90));
    }
}
