package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateSiteRequest;
import com.keystone.dto.SitePageResponse;
import com.keystone.dto.SiteResponse;
import com.keystone.dto.UpdateSiteRequest;
import com.keystone.enums.SiteStatus;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.SiteService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SiteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class SiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SiteService siteService;

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

    private SiteResponse siteResponse;

    @BeforeEach
    void setUp() {
        siteResponse = SiteResponse.builder()
                .id(5L)
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customerId(10L)
                .customerCode("ACME001")
                .customerName("Acme Facilities")
                .status(SiteStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createSite_WhenValid_ShouldReturn201() throws Exception {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customerId(10L)
                .contactEmail("hq@acme.com")
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_SITE"))).thenReturn(true);
        when(siteService.createSite(any(CreateSiteRequest.class))).thenReturn(siteResponse);

        mockMvc.perform(post("/api/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siteCode").value("HQ-01"))
                .andExpect(jsonPath("$.customerId").value(10));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createSite_WhenValidationFails_ShouldReturn400() throws Exception {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode(" ")
                .siteName("")
                .contactEmail("bad-email")
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_SITE"))).thenReturn(true);

        mockMvc.perform(post("/api/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.siteCode").exists())
                .andExpect(jsonPath("$.validationErrors.siteName").exists())
                .andExpect(jsonPath("$.validationErrors.customerId").exists())
                .andExpect(jsonPath("$.validationErrors.contactEmail").exists());
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createSite_WhenDuplicateCode_ShouldReturn409() throws Exception {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customerId(10L)
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_SITE"))).thenReturn(true);
        when(siteService.createSite(any(CreateSiteRequest.class)))
                .thenThrow(new DuplicateResourceException("A site with code HQ-01 already exists."));

        mockMvc.perform(post("/api/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A site with code HQ-01 already exists."));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createSite_WhenCustomerMissing_ShouldReturn404() throws Exception {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode("WH-02")
                .siteName("Warehouse")
                .customerId(99L)
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_SITE"))).thenReturn(true);
        when(siteService.createSite(any(CreateSiteRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 99"));

        mockMvc.perform(post("/api/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getSite_WhenExists_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_SITE"))).thenReturn(true);
        when(siteService.getSiteById(5L)).thenReturn(siteResponse);

        mockMvc.perform(get("/api/sites/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("Headquarters"))
                .andExpect(jsonPath("$.customerCode").value("ACME001"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getSites_ShouldSupportSearchStatusAndCustomerFilter() throws Exception {
        SitePageResponse pageResponse = SitePageResponse.builder()
                .content(List.of(siteResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(authorizationService.hasPermission(any(), eq("VIEW_SITE"))).thenReturn(true);
        when(siteService.getSites(0, 10, "createdAt", "hq", SiteStatus.ACTIVE, 10L)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/sites")
                        .param("search", "hq")
                        .param("status", "ACTIVE")
                        .param("customerId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].siteCode").value("HQ-01"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void updateSite_ShouldReturn200() throws Exception {
        UpdateSiteRequest request = UpdateSiteRequest.builder()
                .siteCode("HQ-01")
                .siteName("Main Campus")
                .customerId(10L)
                .build();

        siteResponse.setSiteName("Main Campus");
        when(authorizationService.hasPermission(any(), eq("UPDATE_SITE"))).thenReturn(true);
        when(siteService.updateSite(eq(5L), any(UpdateSiteRequest.class))).thenReturn(siteResponse);

        mockMvc.perform(put("/api/sites/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteName").value("Main Campus"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void updateSiteStatus_ShouldReturn200() throws Exception {
        siteResponse.setStatus(SiteStatus.INACTIVE);
        when(authorizationService.hasPermission(any(), eq("UPDATE_SITE"))).thenReturn(true);
        when(siteService.updateSiteStatus(5L, SiteStatus.INACTIVE)).thenReturn(siteResponse);

        mockMvc.perform(patch("/api/sites/5/status").param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void deleteSite_ShouldReturn204() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_SITE"))).thenReturn(true);

        mockMvc.perform(delete("/api/sites/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void createSite_WhenTechnician_ShouldReturn403() throws Exception {
        CreateSiteRequest request = CreateSiteRequest.builder()
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customerId(10L)
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_SITE"))).thenReturn(false);

        mockMvc.perform(post("/api/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
