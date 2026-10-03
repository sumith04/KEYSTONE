package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateServiceRequest;
import com.keystone.dto.CustomerProfileResponse;
import com.keystone.dto.ServiceRequestResponse;
import com.keystone.enums.CustomerStatus;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.service.AuthorizationService;
import com.keystone.service.CustomerPortalService;
import com.keystone.service.ServiceRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerPortalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class CustomerPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerPortalService customerPortalService;
    @MockBean
    private ServiceRequestService serviceRequestService;
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
    @WithMockUser(username = "acme@keystone.com", roles = {"CUSTOMER"})
    void getProfile_ShouldReturnCustomerFacingFields() throws Exception {
        when(customerPortalService.getProfile("acme@keystone.com"))
                .thenReturn(CustomerProfileResponse.builder()
                        .id(1L)
                        .companyName("Acme Facilities")
                        .status(CustomerStatus.ACTIVE)
                        .build());

        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Facilities"));
    }

    @Test
    @WithMockUser(username = "acme@keystone.com", roles = {"CUSTOMER"})
    void createRequest_ShouldIgnoreClientCustomerIdAndUsePrincipal() throws Exception {
        when(authorizationService.hasPermission(any(), eq("REQUEST_RAISE"))).thenReturn(true);
        when(serviceRequestService.createMyRequest(any(CreateServiceRequest.class), eq("acme@keystone.com")))
                .thenReturn(ServiceRequestResponse.builder()
                        .id(50L)
                        .requestNumber("SR-000050")
                        .status(ServiceRequestStatus.SUBMITTED)
                        .priority(WorkOrderPriority.HIGH)
                        .build());

        mockMvc.perform(post("/api/customer/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"siteId":10,"title":"HVAC repair","description":"Lobby unit failed","priority":"HIGH","customerId":99,"status":"CONVERTED_TO_WORK_ORDER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestNumber").value("SR-000050"));

        verify(serviceRequestService).createMyRequest(any(CreateServiceRequest.class), eq("acme@keystone.com"));
    }
}
