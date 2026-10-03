package com.keystone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.config.CustomUserDetailsService;
import com.keystone.config.JwtAuthenticationFilter;
import com.keystone.config.JwtTokenProvider;
import com.keystone.config.MethodSecurityTestConfig;
import com.keystone.config.RolePermissionMapper;
import com.keystone.config.TokenBlacklistService;
import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.UpdateWorkOrderRequest;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.enums.WorkType;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.service.AuthorizationService;
import com.keystone.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkOrderService workOrderService;

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

    private WorkOrderResponse workOrderResponse;

    @BeforeEach
    void setUp() {
        workOrderResponse = WorkOrderResponse.builder()
                .id(7L)
                .workOrderNumber("WO-000007")
                .title("Replace HVAC filter")
                .customerId(1L)
                .customerName("Acme Facilities")
                .siteId(10L)
                .siteName("Headquarters")
                .status(WorkOrderStatus.NEW)
                .priority(WorkOrderPriority.HIGH)
                .workType(WorkType.PREVENTIVE_MAINTENANCE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void createWorkOrder_WhenValid_ShouldReturn201() throws Exception {
        CreateWorkOrderRequest request = validCreateRequest();
        when(authorizationService.hasPermission(any(), eq("CREATE_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.createWorkOrder(any(CreateWorkOrderRequest.class), eq("dispatcher@keystone.com")))
                .thenReturn(workOrderResponse);

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workOrderNumber").value("WO-000007"));
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void createWorkOrder_WhenValidationFails_ShouldReturn400() throws Exception {
        CreateWorkOrderRequest request = CreateWorkOrderRequest.builder()
                .title(" ")
                .priority(null)
                .build();

        when(authorizationService.hasPermission(any(), eq("CREATE_WORK_ORDER"))).thenReturn(true);

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed for request parameters"))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.customerId").exists())
                .andExpect(jsonPath("$.validationErrors.siteId").exists())
                .andExpect(jsonPath("$.validationErrors.priority").exists())
                .andExpect(jsonPath("$.validationErrors.workType").exists());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void createWorkOrder_WhenScheduleInvalid_ShouldReturn400() throws Exception {
        CreateWorkOrderRequest request = validCreateRequest();
        request.setScheduledStart(LocalDateTime.of(2026, 10, 4, 16, 0));
        request.setScheduledEnd(LocalDateTime.of(2026, 10, 4, 9, 0));

        when(authorizationService.hasPermission(any(), eq("CREATE_WORK_ORDER"))).thenReturn(true);

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.scheduleValid").exists());
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getWorkOrder_WhenExists_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrderById(7L, "admin@keystone.com")).thenReturn(workOrderResponse);

        mockMvc.perform(get("/api/work-orders/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Replace HVAC filter"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getWorkOrder_WhenMissing_ShouldReturn404() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrderById(99L, "admin@keystone.com"))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 99"));

        mockMvc.perform(get("/api/work-orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Work order not found with id: 99"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getWorkOrderByNumber_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrderByNumber("WO-000007", "admin@keystone.com")).thenReturn(workOrderResponse);

        mockMvc.perform(get("/api/work-orders/number/WO-000007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workOrderNumber").value("WO-000007"));
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getWorkOrders_ShouldSupportFilters() throws Exception {
        WorkOrderPageResponse pageResponse = WorkOrderPageResponse.builder()
                .content(List.of(workOrderResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrders(
                0, 10, "createdAt,desc", "hvac", WorkOrderStatus.NEW, WorkOrderPriority.HIGH, 1L, 10L, 200L, "admin@keystone.com"))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/work-orders")
                        .param("search", "hvac")
                        .param("status", "NEW")
                        .param("priority", "HIGH")
                        .param("customerId", "1")
                        .param("siteId", "10")
                        .param("technicianId", "200")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].workOrderNumber").value("WO-000007"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void updateWorkOrder_ShouldReturn200() throws Exception {
        UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                .title("Updated HVAC work")
                .customerId(1L)
                .siteId(10L)
                .priority(WorkOrderPriority.URGENT)
                .workType(WorkType.EMERGENCY)
                .build();
        workOrderResponse.setTitle("Updated HVAC work");

        when(authorizationService.hasPermission(any(), eq("UPDATE_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.updateWorkOrder(eq(7L), any(UpdateWorkOrderRequest.class), eq("dispatcher@keystone.com"))).thenReturn(workOrderResponse);

        mockMvc.perform(put("/api/work-orders/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated HVAC work"));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void assignWorkOrder_ShouldReturn200() throws Exception {
        workOrderResponse.setStatus(WorkOrderStatus.ASSIGNED);
        workOrderResponse.setAssignedTechnicianId(200L);
        when(authorizationService.hasPermission(any(), eq("ASSIGN_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.assignWorkOrder(eq(7L), any(AssignWorkOrderRequest.class), eq("manager@keystone.com"))).thenReturn(workOrderResponse);

        mockMvc.perform(post("/api/work-orders/7/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignWorkOrderRequest.builder().technicianId(200L).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void assignWorkOrder_WhenTechnicianMissing_ShouldReturn400() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ASSIGN_WORK_ORDER"))).thenReturn(true);

        mockMvc.perform(post("/api/work-orders/7/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.technicianId").exists());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void startHoldResumeComplete_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("START_WORK"))).thenReturn(true);
        when(authorizationService.hasPermission(any(), eq("HOLD_WORK"))).thenReturn(true);
        when(authorizationService.hasPermission(any(), eq("RESUME_WORK"))).thenReturn(true);
        when(authorizationService.hasPermission(any(), eq("COMPLETE_WORK"))).thenReturn(true);

        workOrderResponse.setStatus(WorkOrderStatus.IN_PROGRESS);
        when(workOrderService.startWorkOrder(7L, "technician@keystone.com")).thenReturn(workOrderResponse);
        when(workOrderService.holdWorkOrder(7L, "technician@keystone.com")).thenReturn(workOrderResponse);
        when(workOrderService.resumeWorkOrder(7L, "technician@keystone.com")).thenReturn(workOrderResponse);
        when(workOrderService.completeWorkOrder(7L, "technician@keystone.com")).thenReturn(workOrderResponse);

        mockMvc.perform(post("/api/work-orders/7/start")).andExpect(status().isOk());
        mockMvc.perform(post("/api/work-orders/7/hold")).andExpect(status().isOk());
        mockMvc.perform(post("/api/work-orders/7/resume")).andExpect(status().isOk());
        mockMvc.perform(post("/api/work-orders/7/complete")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void closeAndCancel_ShouldReturn200() throws Exception {
        when(authorizationService.hasPermission(any(), eq("CLOSE_WORK_ORDER"))).thenReturn(true);
        when(authorizationService.hasPermission(any(), eq("CANCEL_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.closeWorkOrder(7L, "manager@keystone.com")).thenReturn(workOrderResponse);
        when(workOrderService.cancelWorkOrder(7L, "manager@keystone.com")).thenReturn(workOrderResponse);

        mockMvc.perform(post("/api/work-orders/7/close")).andExpect(status().isOk());
        mockMvc.perform(post("/api/work-orders/7/cancel")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void completeWorkOrder_WhenInvalidTransition_ShouldReturn409() throws Exception {
        when(authorizationService.hasPermission(any(), eq("COMPLETE_WORK"))).thenReturn(true);
        when(workOrderService.completeWorkOrder(7L, "technician@keystone.com"))
                .thenThrow(new ApiException("Invalid work order status transition from ASSIGNED to COMPLETED.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/work-orders/7/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid work order status transition from ASSIGNED to COMPLETED."));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void deleteWorkOrder_WhenClosed_ShouldReturn409() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_WORK_ORDER"))).thenReturn(true);
        doThrow(new ApiException("Completed or closed work orders cannot be deleted.", HttpStatus.CONFLICT))
                .when(workOrderService).deleteWorkOrder(7L, "manager@keystone.com");

        mockMvc.perform(delete("/api/work-orders/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Completed or closed work orders cannot be deleted."));
    }

    @Test
    @WithMockUser(username = "manager@keystone.com", roles = {"MANAGER"})
    void deleteWorkOrder_WhenAuthorized_ShouldReturn204() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_WORK_ORDER"))).thenReturn(true);

        mockMvc.perform(delete("/api/work-orders/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void createWorkOrder_WhenCustomerSiteMismatch_ShouldReturn400() throws Exception {
        when(authorizationService.hasPermission(any(), eq("CREATE_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.createWorkOrder(any(CreateWorkOrderRequest.class), eq("dispatcher@keystone.com")))
                .thenThrow(new ApiException("The selected site does not belong to the selected customer.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The selected site does not belong to the selected customer."));
    }

    @Test
    @WithMockUser(username = "customer@keystone.com", roles = {"CUSTOMER"})
    void getWorkOrders_WhenCustomerRole_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(false);

        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void createWorkOrder_WhenTechnician_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("CREATE_WORK_ORDER"))).thenReturn(false);

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void deleteWorkOrder_WhenDispatcher_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("DELETE_WORK_ORDER"))).thenReturn(false);

        mockMvc.perform(delete("/api/work-orders/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcher@keystone.com", roles = {"DISPATCHER"})
    void startWorkOrder_WhenDispatcher_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("START_WORK"))).thenReturn(false);

        mockMvc.perform(post("/api/work-orders/7/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void assignWorkOrder_WhenTechnician_ShouldReturn403() throws Exception {
        when(authorizationService.hasPermission(any(), eq("ASSIGN_WORK_ORDER"))).thenReturn(false);

        mockMvc.perform(post("/api/work-orders/7/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AssignWorkOrderRequest.builder().technicianId(200L).build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void getWorkOrder_WhenNotAssignedToTechnician_ShouldReturn404() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrderById(8L, "technician@keystone.com"))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 8"));

        mockMvc.perform(get("/api/work-orders/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Work order not found with id: 8"));
    }

    @Test
    @WithMockUser(username = "technician@keystone.com", roles = {"TECHNICIAN"})
    void startWorkOrder_WhenNotAssignedToTechnician_ShouldReturn404() throws Exception {
        when(authorizationService.hasPermission(any(), eq("START_WORK"))).thenReturn(true);
        when(workOrderService.startWorkOrder(8L, "technician@keystone.com"))
                .thenThrow(new ResourceNotFoundException("Work order not found with id: 8"));

        mockMvc.perform(post("/api/work-orders/8/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@keystone.com", roles = {"ADMIN"})
    void getWorkOrders_DefaultSort_ShouldPassCreatedAt() throws Exception {
        when(authorizationService.hasPermission(any(), eq("VIEW_WORK_ORDER"))).thenReturn(true);
        when(workOrderService.getWorkOrders(0, 10, "createdAt", null, null, null, null, null, null, "admin@keystone.com"))
                .thenReturn(WorkOrderPageResponse.builder().content(List.of()).page(0).size(10).totalElements(0).totalPages(0).build());

        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private CreateWorkOrderRequest validCreateRequest() {
        return CreateWorkOrderRequest.builder()
                .title("Replace HVAC filter")
                .description("Quarterly filter replacement")
                .customerId(1L)
                .siteId(10L)
                .priority(WorkOrderPriority.HIGH)
                .workType(WorkType.PREVENTIVE_MAINTENANCE)
                .scheduledStart(LocalDateTime.of(2026, 10, 4, 9, 0))
                .scheduledEnd(LocalDateTime.of(2026, 10, 4, 12, 0))
                .notes("Bring extra filters")
                .build();
    }
}
