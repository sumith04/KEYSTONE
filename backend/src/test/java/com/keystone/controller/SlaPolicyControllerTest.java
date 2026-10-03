package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateSlaPolicyRequest;
import com.keystone.dto.SlaPolicyPageResponse;
import com.keystone.dto.SlaPolicyResponse;
import com.keystone.dto.UpdateSlaPolicyRequest;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.service.AuthorizationService;
import com.keystone.service.SlaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SlaPolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class SlaPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SlaService slaService;

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

    private SlaPolicyResponse policyResponse;

    @BeforeEach
    void setUp() {
        policyResponse = SlaPolicyResponse.builder()
                .id(3L)
                .name("Gold")
                .priority(WorkOrderPriority.HIGH)
                .responseTimeMinutes(60)
                .resolutionTimeMinutes(240)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createPolicy_WhenValid_ShouldReturn201() throws Exception {
        when(authorizationService.hasPermission(any(), eq("CREATE_SLA"))).thenReturn(true);
        when(slaService.createPolicy(any(CreateSlaPolicyRequest.class))).thenReturn(policyResponse);

        mockMvc.perform(post("/api/sla-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gold"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createPolicy_WhenInvalidDurations_ShouldReturn400() throws Exception {
        when(authorizationService.hasPermission(any(), eq("CREATE_SLA"))).thenReturn(true);

        CreateSlaPolicyRequest request = CreateSlaPolicyRequest.builder()
                .name("Bad")
                .priority(WorkOrderPriority.LOW)
                .responseTimeMinutes(120)
                .resolutionTimeMinutes(30)
                .build();

        mockMvc.perform(post("/api/sla-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.durationValid").exists());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getPolicies_WhenAuthorized_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_SLA"))).thenReturn(true);
        when(slaService.getPolicies(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(SlaPolicyPageResponse.builder()
                        .content(List.of(policyResponse))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/api/sla-policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Gold"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void updatePolicy_WhenValid_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("UPDATE_SLA"))).thenReturn(true);
        when(slaService.updatePolicy(eq(3L), any(UpdateSlaPolicyRequest.class))).thenReturn(policyResponse);

        mockMvc.perform(put("/api/sla-policies/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateSlaPolicyRequest.builder()
                                .name("Gold")
                                .priority(WorkOrderPriority.HIGH)
                                .responseTimeMinutes(60)
                                .resolutionTimeMinutes(240)
                                .active(true)
                                .build())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void deletePolicy_WhenAuthorized_ShouldReturn204() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_SLA"))).thenReturn(true);
        doNothing().when(slaService).deletePolicy(3L);

        mockMvc.perform(delete("/api/sla-policies/3"))
                .andExpect(status().isNoContent());
    }

    private CreateSlaPolicyRequest validCreateRequest() {
        return CreateSlaPolicyRequest.builder()
                .name("Gold")
                .priority(WorkOrderPriority.HIGH)
                .responseTimeMinutes(60)
                .resolutionTimeMinutes(240)
                .active(true)
                .build();
    }
}
