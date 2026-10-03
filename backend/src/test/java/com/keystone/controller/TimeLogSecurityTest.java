package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateTimeLogRequest;
import com.keystone.service.AuthorizationService;
import com.keystone.service.TimeLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TimeLogController.class)
@Import(MethodSecurityTestConfig.class)
class TimeLogSecurityTest {

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
    void getTimeLogs_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/work-orders/7/time-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void addTimeLog_WhenCustomer_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_TIME_LOG"))).thenReturn(false);

        mockMvc.perform(post("/api/work-orders/7/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateTimeLogRequest.builder()
                                .startTime(LocalDateTime.of(2026, 10, 3, 9, 0))
                                .endTime(LocalDateTime.of(2026, 10, 3, 10, 0))
                                .build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void getTimeLogs_WhenDispatcher_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_TIME_LOGS"))).thenReturn(false);

        mockMvc.perform(get("/api/work-orders/7/time-logs"))
                .andExpect(status().isForbidden());
    }
}
