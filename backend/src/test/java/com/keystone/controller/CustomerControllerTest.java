package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.CreateCustomerRequest;
import com.keystone.dto.CustomerPageResponse;
import com.keystone.dto.CustomerResponse;
import com.keystone.dto.UpdateCustomerRequest;
import com.keystone.enums.CustomerStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @MockBean
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

    private CustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        customerResponse = CustomerResponse.builder()
                .id(1L)
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .email("jane.doe@acme.com")
                .status(CustomerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createCustomer_WhenValid_ShouldReturn201() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .email("jane.doe@acme.com")
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_CUSTOMER"))).thenReturn(true);
        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerCode").value("ACME001"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createCustomer_WhenValidationFails_ShouldReturn400() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode(" ")
                .companyName("")
                .email("not-an-email")
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_CUSTOMER"))).thenReturn(true);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed for request parameters"))
                .andExpect(jsonPath("$.validationErrors.customerCode").exists())
                .andExpect(jsonPath("$.validationErrors.companyName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createCustomer_WhenDuplicateCode_ShouldReturn409() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_CUSTOMER"))).thenReturn(true);
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("A customer with code ACME001 already exists."));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A customer with code ACME001 already exists."));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void createCustomer_WhenDuplicateEmail_ShouldReturn409() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode("ACME002")
                .companyName("Acme East")
                .email("jane.doe@acme.com")
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_CUSTOMER"))).thenReturn(true);
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("A customer with email address jane.doe@acme.com already exists."));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A customer with email address jane.doe@acme.com already exists."));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getCustomer_WhenExists_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_CUSTOMER"))).thenReturn(true);
        when(customerService.getCustomerById(1L)).thenReturn(customerResponse);

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Acme Facilities"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getCustomer_WhenMissing_ShouldReturn404() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_CUSTOMER"))).thenReturn(true);
        when(customerService.getCustomerById(99L))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 99"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getCustomers_ShouldSupportSearchAndStatusFilter() throws Exception {
        CustomerPageResponse pageResponse = CustomerPageResponse.builder()
                .content(List.of(customerResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(authorizationService.hasPermission(any(), eq("VIEW_CUSTOMER"))).thenReturn(true);
        when(customerService.getCustomers(0, 10, "createdAt", "acme", CustomerStatus.ACTIVE)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/customers")
                        .param("search", "acme")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerCode").value("ACME001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void updateCustomer_ShouldReturn200() throws Exception {
        UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                .customerCode("ACME001")
                .companyName("Acme Facilities LLC")
                .build();

        customerResponse.setCompanyName("Acme Facilities LLC");
        when(authorizationService.hasPermission(any(), eq("UPDATE_CUSTOMER"))).thenReturn(true);
        when(customerService.updateCustomer(eq(1L), any(UpdateCustomerRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Facilities LLC"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void updateCustomerStatus_ShouldReturn200() throws Exception {
        customerResponse.setStatus(CustomerStatus.INACTIVE);
        when(authorizationService.hasPermission(any(), eq("UPDATE_CUSTOMER"))).thenReturn(true);
        when(customerService.updateCustomerStatus(1L, CustomerStatus.INACTIVE)).thenReturn(customerResponse);

        mockMvc.perform(patch("/api/customers/1/status").param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void deleteCustomer_WhenSitesExist_ShouldReturn409() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_CUSTOMER"))).thenReturn(true);
        doThrow(new ApiException("Cannot delete customer because one or more sites are associated with it.", HttpStatus.CONFLICT))
                .when(customerService).deleteCustomer(1L);

        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete customer because one or more sites are associated with it."));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void deleteCustomer_WhenAuthorized_ShouldReturn204() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_CUSTOMER"))).thenReturn(true);

        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void getCustomers_WhenUnauthorizedRole_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_CUSTOMER"))).thenReturn(false);

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void createCustomer_WhenCustomerRole_ShouldReturn403() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_CUSTOMER"))).thenReturn(false);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
