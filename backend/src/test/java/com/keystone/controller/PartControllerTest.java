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
import com.keystone.dto.PartResponse;
import com.keystone.dto.UpdatePartRequest;
import com.keystone.enums.PartStatus;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.PartService;
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

import java.math.BigDecimal;
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

@WebMvcTest(controllers = PartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class PartControllerTest {

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

    private PartResponse partResponse;

    @BeforeEach
    void setUp() {
        partResponse = PartResponse.builder()
                .id(10L)
                .partNumber("FLT-100")
                .name("HVAC Filter")
                .unitCost(new BigDecimal("12.50"))
                .quantityInStock(20)
                .reorderLevel(5)
                .status(PartStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createPart_WhenValid_ShouldReturn201() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_PART"))).thenReturn(true);
        when(partService.createPart(any(CreatePartRequest.class), eq("admin@keystone.com"))).thenReturn(partResponse);

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partNumber").value("FLT-100"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createPart_WhenValidationFails_ShouldReturn400() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_PART"))).thenReturn(true);

        CreatePartRequest request = CreatePartRequest.builder()
                .partNumber(" ")
                .name(" ")
                .unitCost(new BigDecimal("-1"))
                .quantityInStock(-4)
                .reorderLevel(-1)
                .build();

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.partNumber").exists())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.unitCost").exists())
                .andExpect(jsonPath("$.validationErrors.quantityInStock").exists())
                .andExpect(jsonPath("$.validationErrors.reorderLevel").exists());
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createPart_WhenDuplicateNumber_ShouldReturn409() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ADD_PART"))).thenReturn(true);
        when(partService.createPart(any(CreatePartRequest.class), eq("admin@keystone.com")))
                .thenThrow(new DuplicateResourceException("A part with number FLT-100 already exists."));

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A part with number FLT-100 already exists."));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void getParts_WhenAuthorized_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_PART"))).thenReturn(true);
        when(partService.getParts(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(PartPageResponse.builder()
                        .content(List.of(partResponse))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].partNumber").value("FLT-100"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void updatePart_WhenValid_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("UPDATE_PART"))).thenReturn(true);
        when(partService.updatePart(eq(10L), any(UpdatePartRequest.class), eq("admin@keystone.com")))
                .thenReturn(partResponse);

        mockMvc.perform(put("/api/parts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void deletePart_WhenAuthorized_ShouldReturn204() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_PART"))).thenReturn(true);
        doNothing().when(partService).deletePart(10L, "admin@keystone.com");

        mockMvc.perform(delete("/api/parts/10"))
                .andExpect(status().isNoContent());
    }

    private CreatePartRequest validCreateRequest() {
        return CreatePartRequest.builder()
                .partNumber("FLT-100")
                .name("HVAC Filter")
                .unitCost(new BigDecimal("12.50"))
                .quantityInStock(20)
                .reorderLevel(5)
                .status(PartStatus.ACTIVE)
                .build();
    }

    private UpdatePartRequest validUpdateRequest() {
        return UpdatePartRequest.builder()
                .partNumber("FLT-100")
                .name("HVAC Filter")
                .unitCost(new BigDecimal("12.50"))
                .quantityInStock(20)
                .reorderLevel(5)
                .status(PartStatus.ACTIVE)
                .build();
    }
}
