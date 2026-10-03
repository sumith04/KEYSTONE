package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateTimeLogRequest;
import com.keystone.dto.TimeLogResponse;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.TimeLogService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TimeLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class TimeLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimeLogService timeLogService;

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
    void addTimeLog_WhenValid_ShouldReturn201() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_TIME_LOG"))).thenReturn(true);
        when(timeLogService.addTimeLog(eq(7L), any(CreateTimeLogRequest.class), eq("tech.a@keystone.com")))
                .thenReturn(TimeLogResponse.builder()
                        .id(80L)
                        .workOrderId(7L)
                        .technicianId(25L)
                        .technicianName("Terry Alpha")
                        .durationMinutes(150)
                        .build());

        mockMvc.perform(post("/api/work-orders/7/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durationMinutes").value(150))
                .andExpect(jsonPath("$.technicianId").value(25));
    }

    @Test
    @WithMockUser(username = "tech.a@keystone.com", roles = {"TECHNICIAN"})
    void addTimeLog_WhenInvalidRange_ShouldReturn400() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_TIME_LOG"))).thenReturn(true);

        CreateTimeLogRequest request = CreateTimeLogRequest.builder()
                .startTime(LocalDateTime.of(2026, 10, 3, 11, 0))
                .endTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                .build();

        mockMvc.perform(post("/api/work-orders/7/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.timeRangeValid").exists());
    }

    @Test
    @WithMockUser(username = "tech.b@keystone.com", roles = {"TECHNICIAN"})
    void addTimeLog_WhenOtherTechnicianWorkOrder_ShouldReturn404() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_TIME_LOG"))).thenReturn(true);
        when(timeLogService.addTimeLog(eq(7L), any(CreateTimeLogRequest.class), eq("tech.b@keystone.com")))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 7"));

        mockMvc.perform(post("/api/work-orders/7/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "tech.a@keystone.com", roles = {"TECHNICIAN"})
    void addTimeLog_WhenClosed_ShouldReturn409() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_TIME_LOG"))).thenReturn(true);
        when(timeLogService.addTimeLog(eq(7L), any(CreateTimeLogRequest.class), eq("tech.a@keystone.com")))
                .thenThrow(new ApiException(
                        "Time logs cannot be created or changed on a closed or cancelled work order.",
                        HttpStatus.CONFLICT
                ));

        mockMvc.perform(post("/api/work-orders/7/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getTimeLogs_WhenAuthorized_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_TIME_LOGS"))).thenReturn(true);
        when(timeLogService.getTimeLogs(7L, "manager@keystone.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/work-orders/7/time-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private CreateTimeLogRequest validRequest() {
        return CreateTimeLogRequest.builder()
                .startTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                .endTime(LocalDateTime.of(2026, 10, 3, 11, 30))
                .notes("On site")
                .build();
    }
}
