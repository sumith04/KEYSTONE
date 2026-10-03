package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreatePartRequest;
import com.keystone.dto.PartPageResponse;
import com.keystone.enums.PartStatus;
import com.keystone.service.AuthorizationService;
import com.keystone.service.PartService;
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

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartController.class)
@Import(MethodSecurityTestConfig.class)
class PartSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartService partService;

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
    void getParts_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void getParts_WhenCustomer_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_PART"))).thenReturn(false);

        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void createPart_WhenDispatcher_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_PART"))).thenReturn(false);

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreatePartRequest.builder()
                                .partNumber("FLT-100")
                                .name("HVAC Filter")
                                .unitCost(new BigDecimal("12.50"))
                                .quantityInStock(5)
                                .reorderLevel(1)
                                .status(PartStatus.ACTIVE)
                                .build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getParts_WhenManager_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_PART"))).thenReturn(true);
        when(partService.getParts(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(PartPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isOk());
    }
}
