package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateWorkOrderPartRequest;
import com.keystone.dto.WorkOrderPartResponse;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.WorkOrderPartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkOrderPartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class WorkOrderPartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkOrderPartService workOrderPartService;

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
    void addWorkOrderPart_WhenValid_ShouldReturn201() throws Exception {
        when(authorizationService.hasPermission(any(), eq("USE_PARTS"))).thenReturn(true);
        when(workOrderPartService.addWorkOrderPart(eq(7L), any(CreateWorkOrderPartRequest.class), eq("tech.a@keystone.com")))
                .thenReturn(WorkOrderPartResponse.builder()
                        .id(50L)
                        .workOrderId(7L)
                        .partId(10L)
                        .partNumber("FLT-100")
                        .quantityUsed(2)
                        .unitCostAtUsage(new BigDecimal("12.50"))
                        .totalCost(new BigDecimal("25.00"))
                        .build());

        mockMvc.perform(post("/api/work-orders/7/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateWorkOrderPartRequest.builder()
                                .partId(10L)
                                .quantity(2)
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCost").value(25.00));
    }

    @Test
    @WithMockUser(username = "tech.a@keystone.com", roles = {"TECHNICIAN"})
    void addWorkOrderPart_WhenQuantityInvalid_ShouldReturn400() throws Exception {
        when(authorizationService.hasPermission(any(), eq("USE_PARTS"))).thenReturn(true);

        mockMvc.perform(post("/api/work-orders/7/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateWorkOrderPartRequest.builder()
                                .partId(10L)
                                .quantity(0)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());
    }

    @Test
    @WithMockUser(username = "tech.a@keystone.com", roles = {"TECHNICIAN"})
    void addWorkOrderPart_WhenInsufficientStock_ShouldReturn409() throws Exception {
        when(authorizationService.hasPermission(any(), eq("USE_PARTS"))).thenReturn(true);
        when(workOrderPartService.addWorkOrderPart(eq(7L), any(CreateWorkOrderPartRequest.class), eq("tech.a@keystone.com")))
                .thenThrow(new ApiException("Insufficient stock for part FLT-100. Available: 1.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/work-orders/7/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateWorkOrderPartRequest.builder()
                                .partId(10L)
                                .quantity(2)
                                .build())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "tech.b@keystone.com", roles = {"TECHNICIAN"})
    void addWorkOrderPart_WhenOtherTechnicianWorkOrder_ShouldReturn404() throws Exception {
        when(authorizationService.hasPermission(any(), eq("USE_PARTS"))).thenReturn(true);
        when(workOrderPartService.addWorkOrderPart(eq(7L), any(CreateWorkOrderPartRequest.class), eq("tech.b@keystone.com")))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 7"));

        mockMvc.perform(post("/api/work-orders/7/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateWorkOrderPartRequest.builder()
                                .partId(10L)
                                .quantity(1)
                                .build())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "tech.a@keystone.com", roles = {"TECHNICIAN"})
    void getWorkOrderParts_WhenAuthorized_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_PART"))).thenReturn(true);
        when(workOrderPartService.getWorkOrderParts(7L, "tech.a@keystone.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/work-orders/7/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
